# Recorded terminal session

This transcript was captured from the packaged JAR on 23 October 2026, with `--seed 42`, from outside the repository directory. The temporary save path is displayed as `saves/My Session.json` for readability; gameplay output and commands are otherwise retained. `Monastary` is the spelling in the supplied card catalogue.

```text
Citadels CLI — a simplified strategy game. Type quit at any prompt.
Enter how many players [4-7]: 4
You are Player 1. Player 2 holds the crown.

=== ROUND 1 — CHARACTER SELECTION ===
One character was discarded face down.
Merchant was discarded face up.
Thief was discarded face up.
Player 2 chose a hidden character.
Player 3 chose a hidden character.
Player 4 chose a hidden character.
Choose a character by name or rank:
1. Assassin
4. King
> 4
You chose King.

Player 3 — Magician
Player 3 collected 2 gold.
Player 3: Built Haunted City for 2 gold.

Player 1 — King
Choose turn resources: gold (+2) or cards (draw 2, keep 1).
resources> gold
Player 1 collected 2 gold.
You gained 0 gold from yellow districts and claimed the crown.
Your turn: hand, build <n>, all, help, or end.
> hand
1. Cathedral [blue5]
2. Map Room [purple5]
3. Monastary [blue3]
4. Observatory [purple5]
> all
Player 1 (you): gold=4, cards=4, districts=0
Player 2: gold=2, cards=4, districts=0
Player 3: gold=2, cards=3, districts=1
Player 4: gold=2, cards=4, districts=0
> info King
King: take the crown and gain one gold per yellow district.
> build 1
Not enough gold.
> build 3
Built Monastary for 3 gold.
> save saves/My Session.json
Game saved to saves/My Session.json
> quit
Goodbye.
```

## Resume in a new process

The saved checkpoint was opened in a fresh JVM. The gold balance and re-saved JSON state matched the checkpoint:

```text
Game loaded from saves/My Session.json
Your turn: hand, build <n>, all, help, or end.
> gold
You have 1 gold.
> save saves/resumed.json
Game saved to saves/resumed.json
> quit
Goodbye.
```
