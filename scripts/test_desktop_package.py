"""Check that missing or empty desktop resources cannot pass the release gate."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

spec = importlib.util.spec_from_file_location("package_check", Path(__file__).with_name("check-desktop-package.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class DesktopPackageTest(unittest.TestCase):
    def check_jar(self, entries):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "desktop.jar"
            with ZipFile(path, "w") as jar:
                for name, data in entries.items():
                    jar.writestr(name, data)
            module.validate(path)

    def test_complete_package(self):
        self.check_jar(dict.fromkeys(module.REQUIRED, b"native-binary"))

    def test_linux_only_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "windows-x86_64.*darwin-x86_64.*darwin-aarch64"):
            self.check_jar({module.REQUIRED[0]: b"linux-binary"})

    def test_empty_binary_is_rejected(self):
        entries = dict.fromkeys(module.REQUIRED, b"native-binary")
        entries[module.REQUIRED[1]] = b""
        with self.assertRaisesRegex(RuntimeError, "windows-x86_64"):
            self.check_jar(entries)

if __name__ == "__main__":
    unittest.main()
