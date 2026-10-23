# Verification record

Verified on **23 October 2026**, using macOS, Microsoft OpenJDK 17.0.16, Gradle 8.10, JUnit 5.10.3 and JaCoCo 0.8.12.

## Complete build

```bash
./gradlew clean build installDist --no-daemon
```

Observed result: **83 tests passed, 0 failures, 0 errors, 0 skipped**. The build produced the runnable JAR, ZIP/TAR distributions and installed launch scripts. Test failures are no longer ignored.

| Area | Tests | Evidence |
| --- | ---: | --- |
| Game | 18 | Resource collection, build transaction rules, automatic computer powers, theft/assassination, Magician/Warlord behavior, score/round progression, command-loop resume and a complete seeded multi-round game ending in final scores |
| Persistence | 15 | Full snapshot round-trip, UTF-8 and spaced filenames, schema/reference validation, old/future format rejection, atomic replacement, failure cleanup and preservation of existing files |
| District catalogue | 24 | Bundled deck totals, valid input variants, malformed data and encoding, missing/read failures and no partial results |
| CLI | 4 | Help/arguments, EOF/quit, missing save errors |
| Models, role powers and card info | 22 | Hand/city/gold behavior, equality, role dispatch and income, lookup and character targets |

The [machine-readable record](verification-results.json) lists each test suite and its counters.

## Measured coverage

The complete local run produced:

| Metric | Covered / total | Coverage |
| --- | ---: | ---: |
| Instruction | 4292 / 4668 | 91.95% |
| Branch | 410 / 528 | 77.65% |
| Line | 724 / 777 | 93.18% |
| Method | 144 / 151 | 95.36% |
| Class | 19 / 19 | 100.00% |

These are observations, not coverage guarantees for all possible strategies or inputs. HTML reports are generated at `build/reports/tests/test/index.html` and `build/reports/jacoco/test/html/index.html`.

## Packaged application checks

The runnable JAR was launched from an unrelated temporary working directory with seed 42. An automated terminal session supplied actual input to the built application: player count, role selection, resources, hand/summary inspection, character information, rejected unaffordable construction, successful construction, save and quit. The [recorded session](DEMO.md) contains that output.

A fresh Java process then loaded the saved file, confirmed the expected gold balance, saved again, and exited. The parsed JSON snapshots before and after this resume were equal, demonstrating that the command-prompt checkpoint did not re-award resources or powers. This test also checks classpath access to the catalogue and JSON runtime dependency outside the project folder.

The installed Unix launcher was separately executed from that temporary directory and exited cleanly on `quit`. The CI workflow repeats the full build and startup/quit smoke checks for the JAR and Unix launcher on Ubuntu / Java 17; its reports are uploaded as artifacts. The detailed packaged session and fresh-process resume checks above were run locally.

## Original baseline

An unchanged source copy ran **22 tests with 1 failure**: the original `AppTest` crashed on end-of-input with `NoSuchElementException`. The build nevertheless reported success because `ignoreFailures = true` was set. The baseline also emitted source-encoding diagnostics under the process's US-ASCII default. Compilation and test encoding are now explicitly UTF-8.

Code review and actual execution additionally identified incomplete round advancement, manual input required for computer powers, recursive turn replay through `t`, missing build limits and incomplete saves. The portfolio edition repairs these paths and verifies them with regression tests. The original source directory was not edited; a local SHA-256 manifest was checked after the work.

## Limits of validation

- The complete-game test uses a fixed random seed and scripted inputs. It proves the round engine can reach final scoring, not that opponent strategy is sophisticated or every possible match terminates in a fixed number of rounds.
- Purple district powers are outside the implemented rule set; tests do not imply support for those rules.
- A checkpoint preserves gameplay state but not the internal random-generator state. Later random decisions need not match an uninterrupted session.
- Local packaging was checked on macOS; CI covers Ubuntu. Generated Windows scripts were not executed locally.
- Atomic saves require support from the target filesystem. A missing parent directory or unsupported atomic move is reported as a failure.
- The tested Gradle 8.10 build reports deprecated behavior that will need attention before upgrading to Gradle 9. Test JSON fixtures also emit an unchecked-generics compiler note. Neither prevented the tested build.
