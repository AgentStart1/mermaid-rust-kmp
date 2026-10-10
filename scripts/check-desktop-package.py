"""Reject a published JVM JAR that is missing any advertised desktop host."""
import sys
from zipfile import ZipFile

REQUIRED = (
    "native/linux-x86_64/libmermaid_ffi_jni.so",
    "native/windows-x86_64/mermaid_ffi_jni.dll",
    "native/darwin-x86_64/libmermaid_ffi_jni.dylib",
    "native/darwin-aarch64/libmermaid_ffi_jni.dylib",
)


def validate(path):
    with ZipFile(path) as jar:
        missing = [name for name in REQUIRED if name not in jar.namelist() or jar.getinfo(name).file_size == 0]
    if missing:
        raise RuntimeError("Missing desktop JNI libraries: " + ", ".join(missing))


if __name__ == "__main__":
    validate(sys.argv[1])
