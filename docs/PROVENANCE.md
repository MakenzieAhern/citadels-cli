# Provenance

This portfolio edition was prepared from the local Java coursework project supplied by Yuchen Deng. Yuchen Deng confirmed that he independently wrote the original game code. This statement does not extend to the game design, supplied card text, or any course-provided scaffolding. The original `Scaffold` directory was left unchanged. Development, testing, and publication use a separate copy named `citadels-cli`.

The supplied files included 15 production Java classes, 14 test classes, a TSV district-card catalogue, a Gradle wrapper, a saved game, and local IDE/build output. There was no Git history, README, or project license. This repository begins with a curated snapshot; it does not reconstruct an earlier commit history.

## What was retained

- The original object model, character-role classes, and district-card catalogue.
- The terminal-based interaction and Java / Gradle implementation.
- Original tests where they meaningfully exercise the implementation.

The supplied catalogue contains Citadels names and descriptive rules text. The game design, names, and any course-provided scaffolding are distinct from the implementation work. This is an independent educational implementation, not an official Citadels product.

## Publication preparation

The portfolio edition repairs the round/turn flow, input handling, opponent behavior, build constraints and persistence, and adds regression tests, portable packaging, CI, and documentation. The README describes its simplified rules and unimplemented card effects.

IDE settings, caches, compiled output, operating-system metadata, the redundant source ZIP, and the original personal save file are excluded. New saves should be stored in the ignored `saves/` directory.

## Attribution and licensing

The portfolio repository is maintained by Yuchen Deng. No license or attribution manifest was included with the supplied project, so this edition does not assign a blanket open-source license to the inherited rules text or scaffolding. Dependencies retain their respective licenses.
