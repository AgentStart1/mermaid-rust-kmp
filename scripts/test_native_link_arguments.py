"""Rust CI color output must never become part of a native linker argument."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("jni_build", Path(__file__).with_name("build-desktop-jni.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class NativeLinkArgumentsTest(unittest.TestCase):
    def test_colored_linux_dependencies(self):
        output = "\x1b[1mnote: native-static-libs: -lpthread -lm -lc\x1b[0m\n"
        self.assertEqual(["-lpthread", "-lm", "-lc"], module.native_link_libraries(output))

    def test_frameworks_and_duplicate_order_are_preserved(self):
        output = "native-static-libs: -framework Security -lSystem -lc -lSystem\n"
        self.assertEqual(["-framework", "Security", "-lSystem", "-lc", "-lSystem"], module.native_link_libraries(output))

    def test_windows_linker_options_are_preserved(self):
        output = "native-static-libs: kernel32.lib /defaultlib:msvcrt\x1b[0m\n"
        self.assertEqual(["kernel32.lib", "/defaultlib:msvcrt"], module.native_link_libraries(output))

    def test_missing_dependencies_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "native link dependencies"):
            module.native_link_libraries("Finished release build")

if __name__ == "__main__":
    unittest.main()
