# vexo-fabric

![release](https://img.shields.io/github/v/release/MagischeTraube/vexo-fabric?color=blue&label=Release)
![downloads](https://img.shields.io/github/downloads/MagischeTraube/vexo-fabric/total?color=blue&label=Downloads)
![license](https://img.shields.io/github/license/MagischeTraube/vexo-fabric?color=blue&label=License)
[![discord](https://img.shields.io/discord/1385629270352859296?color=blue&label=Discord)](https://discord.gg/wfW3aEEpVA)

A Hypixel SkyBlock mod for Minecraft 1.21 built on Fabric, focused on Dungeons, Kuudra, and Quality of Life improvements.

> [!NOTE]
> If you have a GitHub account, **please consider leaving a ⭐ Star** and forking the repo! It helps us get more feedback and deliver better features.

---

## Features

<details>
<summary>Dungeons</summary>

| Feature | Description |
|---|---|
| **Healer P5 Leap Alert** | Displays a HUD message when all players have leaped to P5 in M7 — customizable text |
| **Rag Axe Alert** | Shows a HUD title when it's time to use the Rag Axe |
| **Pad Timer** | Calculates when to crush Storm based on your chosen crush order (Green-Yellow or Purple-Yellow) |
| **Particle Hider** | Hides distracting particles during dungeon runs |
| **Party Finder** | Enhances the Party Finder UI with extra player info (secrets, Fairy Perk, etc.) |
| **Positional Messages** | Automatically sends party messages when standing on specific positions in M7 |
| **Auto Dungeon Requeue** | Automatically starts a new run of the current floor after a configurable delay |
| **Storm Pillar Timer** | Displays a HUD title confirming when Storm has been crushed |
| **Necron Block Highlight** | Highlights the exact blocks to mine during the Necron fight |

</details>

<details>
<summary>Kuudra</summary>

| Feature | Description |
|---|---|
| **Auto Kuudra Requeue** | Automatically starts a new Kuudra instance after each run, with configurable delay |
| **Solo Detector** | Alerts your party via `/pc` when all other teammates have been eliminated |
| **Chest Tracker** | Tracks your Kuudra chest count toward the 60-chest limit and displays it on a HUD |
| **Profit Tracker** | Shows the profit of your Kuudra Croesus chests — values the loot, subtracts the cost, and highlights the most profitable chest |
| **Backbone Alert** | HUD bar that tracks Bonemerang backbone timing and tells you exactly when to Rend |
| **Eaten Timer** | Displays a countdown when you get eaten by Kuudra |
| **Fire Veil Overlay** | Shows the Fire Veil radius as a ring and marks the wand in the hotbar while active |
| **Kuudra Party Finder Info** | Shows Kuudra stats of players joining your group via Party Finder |
| **Recolor Lava** | Renders lava as water textures (or a custom color) for better visibility in Kuudra |

</details>

<details>
<summary>Quality of Life</summary>

| Feature | Description |
|---|---|
| **Slayer Helper** | Shows how many Slayer bosses remain and estimates time to the next level |
| **Chat Cleaner** | Filters out repetitive and spammy chat messages (dungeon spam, random spam, etc.) |
| **Auto Rejoin** | Automatically rejoins Hypixel SkyBlock after being kicked |
| **Screenshot Actions** | Adds crop/edit actions to screenshot notifications; supports auto-copy to clipboard |
| **Wardrobe** | Quickly switch armor sets in the Wardrobe using keybinds |
| **Loadouts** | Equip loadouts in the new Loadouts GUI with keybinds |

</details>

<details>
<summary>Commands</summary>

| Command | Description |
|---|---|
| `/vexo` | Opens the mod configuration menu |
| `/tyfr` | Leaves the party after the current Dungeon or Kuudra run ends |
| `/reinvite` | Leaves the party, then messages the party lead "!inv" |
| `/kuudrastats [player]` | Shows Kuudra stats for yourself or the given player |
| `/rewarp <warp>` | Warps to your private island, then to the given warp (useful for resetting warp cooldowns) |
| `/dn` | Warps to the Dungeon Hub |
| `/entrance` | Queues for the Dungeon Entrance |
| `/f1` – `/f7` | Queues for Floor 1–7 |
| `/m1` – `/m7` | Queues for Master Mode Floor 1–7 |
| `/t1` – `/t5` | Queues for Kuudra Tiers 1–5 (e.g. `/t4` = Fiery Kuudra) |

</details>

**More to come!**

> [!NOTE]
> This mod is under active development. Join our [Discord](https://discord.gg/wfW3aEEpVA) to submit feature requests, report bugs, or just hang out.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.1.x.
2. Download and install the required dependencies (see below).
3. Download the latest release of Vexo from the [Releases page](https://github.com/MagischeTraube/vexo-fabric/releases).
4. Place the `.jar` file in your `.minecraft/mods` folder.
5. Launch Minecraft with the Fabric profile.

---

## Dependencies

These mods are **required** and must be placed in your `mods` folder alongside Vexo:

| Dependency | Link |
|---|---|
| **Fabric API** | [Download from Modrinth](https://modrinth.com/mod/fabric-api) |
| **Fabric Language Kotlin** | [Download from Modrinth](https://modrinth.com/mod/fabric-language-kotlin) |

---

## Configuration

Open the config menu in-game with `/vexo` or via the [ModMenu](https://modrinth.com/mod/modmenu) button. Each feature can be individually toggled and configured — HUD elements can be repositioned by clicking **Move HUD** in their settings.


---

## Authors

- **Traube** — [MagischeTraube](https://github.com/MagischeTraube)
- **Lloyd** - [InfernoLloyd](https://github.com/InfernoLloyd)

---

## License

This project is licensed under the **BSD 3-Clause License** — see the [LICENSE](LICENSE) file for details.
