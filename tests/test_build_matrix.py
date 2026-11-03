import json
from pathlib import Path
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]


class MatrixTests(unittest.TestCase):
    def run_plan(self, *args):
        return subprocess.run([sys.executable, str(ROOT / "build_versions.py"), *args],
                              capture_output=True, text=True)

    def test_latest_stable_has_three_loaders(self):
        result = self.run_plan("--latest", "--dry-run")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(result.stdout.splitlines()), 3)
        self.assertNotIn("snapshot", result.stdout.lower())
        self.assertNotIn("26.4", result.stdout)

    def test_selected_version_and_loader(self):
        result = self.run_plan("1.21.8", "--loaders", "quilt", "--dry-run")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(result.stdout.splitlines()), 1)
        self.assertIn("1.21.8", result.stdout)
        self.assertIn("quilt", result.stdout)

    def test_unknown_and_snapshot_versions_rejected(self):
        for version in ("26.4-snapshot-7", "unknown"):
            self.assertNotEqual(self.run_plan(version, "--dry-run").returncode, 0)

    def test_plan_preserves_tracked_configuration(self):
        paths = [ROOT / p for p in ("versions.json", "gradle.properties", "platforms/fabric/fabric.mod.json")]
        before = [p.read_bytes() for p in paths]
        result = self.run_plan("--all", "--dry-run")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(result.stdout.splitlines()), 27)
        self.assertEqual(before, [p.read_bytes() for p in paths])
