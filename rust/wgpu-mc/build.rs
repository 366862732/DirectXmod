use std::path::Path;
use std::process::Command;

fn main() {
    // Embed the current git commit hash so runtime logs can identify which
    // build of wgpu_mc_jni.dll is actually deployed. The JAR-internal DLL
    // overwrites any external copy on every launch, so version confusion
    // (old DLL silently replacing a new one) is easy to miss without this.
    let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap_or_default();
    let hash = Command::new("git")
        .args(["rev-parse", "--short", "HEAD"])
        .current_dir(&manifest_dir)
        .output()
        .ok()
        .filter(|o| o.status.success())
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string())
        .filter(|h| !h.is_empty());

    if let Some(h) = hash {
        println!("cargo:rustc-env=GIT_COMMIT_HASH={}", h);
        // Re-run this script when the git HEAD moves (new commit on any branch).
        let git_dir = Path::new(&manifest_dir).join("../../.git");
        if git_dir.exists() {
            println!("cargo:rerun-if-changed={}", git_dir.display());
        }
    }
}
