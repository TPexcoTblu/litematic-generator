import json
import shutil
import subprocess
from pathlib import Path

# Путь к текущей директории
ROOT = Path(__file__).resolve().parent
VERSIONS_FILE = ROOT / "versions.json"

GRADLE_PROPERTIES_PATH = ROOT / "gradle.properties"
FABRIC_MOD_JSON_PATH = ROOT / "src/main/resources/fabric.mod.json"
OUTPUT_DIR = ROOT / "builds"

# Сохраняем оригинальные тексты, чтобы не портить между итерациями
original_gradle = GRADLE_PROPERTIES_PATH.read_text(encoding="utf-8")
original_fabric_mod = FABRIC_MOD_JSON_PATH.read_text(encoding="utf-8")

# Чтение конфигурации версий
with open(VERSIONS_FILE, encoding="utf-8") as f:
    versions = json.load(f)

for version, config in versions.items():
    print(f"\n--- Building for Minecraft {version} ---")

    # === 1. Обновление gradle.properties ===
    gradle_lines = original_gradle.splitlines()
    updated_gradle = []
    key_values = {**config["gradle.properties"]}

    for line in gradle_lines:
        key = line.split("=", 1)[0].strip()
        if key in key_values:
            line = f"{key}={key_values.pop(key)}"
        updated_gradle.append(line)

    GRADLE_PROPERTIES_PATH.write_text("\n".join(updated_gradle), encoding="utf-8")

    # === 2. Обновление fabric.mod.json ===
    fabric_mod_data = json.loads(original_fabric_mod)
    fabric_mod_data["depends"]["minecraft"] = config["fabric.mod.json"]["minecraft"]
    FABRIC_MOD_JSON_PATH.write_text(json.dumps(fabric_mod_data, indent=4), encoding="utf-8")

    # === 3. Сборка (Windows-совместимо) ===
    try:
        subprocess.run(["gradlew.bat", "clean", "build"], check=True)
    except subprocess.CalledProcessError:
        print(f"❌ Build failed for {version}")
        continue

    # === 4. Поиск JAR ===
    jar_files = list((ROOT / "build/libs").glob("*.jar"))
    if not jar_files:
        print(f"❌ No JAR file found after build for {version}")
        continue

    # === 5. Переименование и копирование jar ===
    out_path = OUTPUT_DIR / version
    out_path.mkdir(parents=True, exist_ok=True)

    # Получаем имя версии мода из gradle.properties
    mod_version_line = next((line for line in updated_gradle if line.startswith("mod_version=")), None)
    mod_version = mod_version_line.split("=", 1)[1].strip() if mod_version_line else "unknown"

    for jar in jar_files:
        base_name = jar.stem  # без .jar
        ext = jar.suffix      # .jar
        new_name = f"{base_name}-mc{version}{ext}"
        target = out_path / new_name
        shutil.copy(jar, target)
        print(f"✅ Saved: {target.name}")

print("\n🎉 Done building all versions.")
