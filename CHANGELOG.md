# Changelog — LocalDream-Enhanced

This fork tracks upstream Local Dream and documents only changes introduced here.

## Unreleased

### Added
- Local prompt templates: save, apply, update by name, and delete paired positive/negative prompts.
- Prompt templates are stored on-device in app-private SharedPreferences; no account or network access is used.
- Focused regression tests for safe model archive replacement.

### Fixed
- ZIP-based model updates now extract into staging before touching the installed model.
- A failed or empty extraction leaves the previously installed model in place.
- Model directory replacement now checks rename results, rolls back when installation fails, and reports if an old backup could not be cleaned up.
- CI runs the focused archive-install regression tests before producing the preview APK.
- Fork attribution and license notes in README.

### Compatibility
- Does not change generation parameters, model files, inference backend, or QNN/NPU configuration.
- Original upstream code remains subject to CC BY-NC 4.0. See LICENSE and README.
