"""Build stable Minecraft targets for Fabric, Quilt and NeoForge."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import hashlib
import struct
import zipfile
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parent
LOADERS = ("fabric", "neoforge", "quilt")


def java_environment(explicit=None):
    env = os.environ.copy()
    candidates = [explicit, env.get("JAVA_HOME_25_X64"), env.get("JAVA_HOME")]
    if os.name == "nt":
        for vendor in ("Eclipse Adoptium", "Java", "Microsoft"):
            candidates.extend(str(p) for p in (Path(os.environ.get("ProgramFiles", "C:/Program Files")) / vendor).glob("*25*"))
    else:
        candidates.extend(str(p) for pattern in ("/usr/lib/jvm/*25*", "/Library/Java/JavaVirtualMachines/*25*/Contents/Home")
                          for p in Path("/").glob(pattern.lstrip("/")))
    for candidate in [*filter(None, candidates), None]:
        java = str(Path(candidate) / "bin" / ("java.exe" if os.name == "nt" else "java")) if candidate else "java"
        try:
            result = subprocess.run([java, "-version"], capture_output=True, text=True)
        except OSError:
            continue
        if re.search(r'version "25[.\"]', result.stderr + result.stdout):
            if candidate:
                env["JAVA_HOME"] = str(candidate)
                env["PATH"] = str(Path(candidate) / "bin") + os.pathsep + env.get("PATH", "")
            return env
    raise RuntimeError("JDK 25 is required. Set JAVA_HOME or pass --java-home /path/to/jdk-25.")


def verify_artifact(path, minecraft, loader, java_version):
    prefix = "com/tpexcotblu/litematicgen/"
    required = [prefix + p for p in ("client/GeneratorScreen.class", "client/GeneratorScreenBase.class",
                "client/MinecraftCompat.class", "client/ClientText.class", "client/GeneratorHotkeys.class", "client/GeneratorSettingsScreen.class", "generators/FormulaGenerator.class",
                "schematic/LitematicWriter.class", "schematic/SchematicVolume.class")]
    required += ["assets/litematic-generator/fonts/Minecraftia-Regular.ttf",
                 "assets/litematic-generator/lang/en_us.json", "assets/litematic-generator/lang/ru_ru.json",
                 "litematic-generator.client.mixins.json"]
    with zipfile.ZipFile(path) as jar:
        missing = set(required) - set(jar.namelist())
        if missing:
            raise ValueError("Missing artifact entries: " + ", ".join(sorted(missing)))
        if loader == "neoforge":
            descriptor = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
            if f'versionRange="[{minecraft}]"' not in descriptor:
                raise ValueError("NeoForge Minecraft version mismatch")
            if "fabric.mod.json" in jar.namelist():
                raise ValueError("Fabric metadata leaked into NeoForge artifact")
        else:
            descriptor = json.loads(jar.read("fabric.mod.json"))
            if descriptor["depends"]["minecraft"] != minecraft or descriptor["environment"] != "client":
                raise ValueError("Fabric/Quilt Minecraft version or side mismatch")
        mixins = json.loads(jar.read("litematic-generator.client.mixins.json"))
        if mixins["compatibilityLevel"] != f"JAVA_{java_version}":
            raise ValueError("Mixin Java compatibility mismatch")
        for name in mixins["client"]:
            mixin_path = mixins["package"].replace(".", "/") + "/" + name + ".class"
            jar.getinfo(mixin_path)
            if minecraft.startswith("1.") and loader != "neoforge":
                expected_method = b"method_1574" if name == "MinecraftMixin" else b"<init>"
                if expected_method not in jar.read(mixin_path):
                    raise ValueError("Legacy mixin selector was not remapped: " + name)
        for entry, major in ((prefix + "client/GeneratorScreen.class", java_version + 44),
                             (prefix + "schematic/SchematicVolume.class", 65)):
            if struct.unpack(">H", jar.read(entry)[6:8])[0] != major:
                raise ValueError("Java class version mismatch: " + entry)
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("versions", nargs="*", help="Versions from versions.json (default: all, newest first)")
    parser.add_argument("--loaders", "--loader", nargs="+", choices=LOADERS, default=list(LOADERS))
    parser.add_argument("--latest", action="store_true", help="Build only the newest stable Minecraft release")
    parser.add_argument("--all", action="store_true", help="Build the whole stable matrix (default)")
    parser.add_argument("--list", action="store_true", help="Show targets and exit")
    parser.add_argument("--dry-run", action="store_true", help="Print the build plan without running Gradle")
    parser.add_argument("--java-home", help="JDK 25 installation")
    args = parser.parse_args()
    configs = json.loads((ROOT / "versions.json").read_text(encoding="utf-8"))
    if args.versions and (args.latest or args.all):
        parser.error("Choose explicit versions, --latest, or --all")
    if args.latest and args.all:
        parser.error("--latest and --all are mutually exclusive")
    selected = args.versions or list(reversed(configs))[:1 if args.latest else None]
    if set(selected) - configs.keys():
        parser.error("Unknown version(s): " + ", ".join(sorted(set(selected) - configs.keys())))
    if any(configs[v].get("type") != "release" for v in selected):
        parser.error("Only release Minecraft versions may be built")
    targets = [(v, loader) for v in selected for loader in dict.fromkeys(args.loaders)]
    if args.list or args.dry_run:
        for version, loader in targets:
            info = configs[version]
            dependency = info.get("neoforge") if loader == "neoforge" else info[loader + "_loader"]
            print(f"{version:8} {loader:9} Java {info['java']}  loader {dependency or 'unavailable'}")
        return 0
    try:
        env = java_environment(args.java_home)
    except RuntimeError as error:
        parser.error(str(error))
    wrapper = [str(ROOT / "gradlew.bat")] if os.name == "nt" else ["sh", str(ROOT / "gradlew")]
    report = {"started_at": datetime.now(timezone.utc).isoformat(), "results": []}
    for version, loader in targets:
        entry = {"minecraft": version, "loader": loader}
        print(f"Building {loader} / Minecraft {version}", flush=True)
        if loader == "neoforge" and not configs[version].get("neoforge"):
            entry.update(status="unavailable", error="No compatible NeoForge release")
        else:
            command = [*wrapper, "build", f"-Ploader={loader}", f"-Pminecraft_version={version}", "--console=plain"]
            logs = ROOT / "builds" / "logs"
            logs.mkdir(parents=True, exist_ok=True)
            log = logs / f"{loader}-{version}.log"
            with log.open("w", encoding="utf-8") as output:
                process = subprocess.Popen(command, cwd=ROOT, env=env, stdout=subprocess.PIPE,
                                           stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
                for line in process.stdout:
                    print(line, end="", flush=True)
                    output.write(line)
                    output.flush()
                code = process.wait()
            source = ROOT / "build" / "targets" / loader / version / "libs"
            jars = list(source.glob("*.jar")) if code == 0 else []
            main_jars = [p for p in jars if not p.name.endswith("-sources.jar")]
            if code or not main_jars:
                entry.update(status="failed", exit_code=code, log=log.relative_to(ROOT).as_posix())
            else:
                try:
                    hashes = {p.name: verify_artifact(p, version, loader, configs[version]["java"]) for p in main_jars}
                except (ValueError, KeyError, zipfile.BadZipFile) as error:
                    entry.update(status="failed", error=str(error), log=log.relative_to(ROOT).as_posix())
                else:
                    destination = ROOT / "builds" / version / loader
                    destination.mkdir(parents=True, exist_ok=True)
                    for jar in jars:
                        shutil.copy2(jar, destination / jar.name)
                    entry.update(status="built", sha256=hashes,
                                 artifacts=[(destination / p.name).relative_to(ROOT).as_posix() for p in main_jars])
        report["results"].append(entry)
        (ROOT / "builds").mkdir(exist_ok=True)
        (ROOT / "builds" / "report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("\n" + "\n".join(f"{r['minecraft']} {r['loader']}: {r['status']}" for r in report["results"]))
    return int(any(r["status"] != "built" for r in report["results"]))


if __name__ == "__main__":
    sys.exit(main())
