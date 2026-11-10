♛ Emperor Client
Fabric client for Minecraft 1.21.4 (Java 21, Fabric API) in the Meteor style: animated ClickGUI, Spotlight search, HUD editor, keybind manager, 12 HUD widgets, commands, macros, profiles, waypoints and 70+ modules.

YOU CAN GET IT HERE: https://github.com/sasabatorin2-collab/Emperor-Client/releases

Interface
ClickGUI: background blur, particles, gradient headers, shadows and glows, smooth opening and closing of settings, animated switches and sliders, click wave, smooth scrolling, hints.
Dock at the bottom of the menu: HUD Editor · Spotlight · Binds · one‑click theme change · reset panel layout.
Spotlight: macOS‑style window — fuzzy module search (Enter — on/off, Shift+Enter — open settings), and if you start with . — hints and command execution.
HUD Editor: dragging widgets with snapping to the edges and center of the screen, RMB — hide/show, LMB — reset, R — reset all. Positions are saved and adjusted to any resolution.
HUD widgets: watermark, module list, info (XYZ / Nether / speed / direction), armor with % durability, Target HUD, radar with a scanner beam, compass with waypoint markers, session statistics (time, kills, deaths, K/D, blocks, distance), keys with CPS, effects with icons and timers.
11 themes (Client → GUI → Theme): Emperor, Amethyst, Crimson, Ocean, Aurora, Sakura, Mint, Sunset, Midnight, Rainbow and Custom (pick both colours with the built-in colour picker).
Commands (prefix .):
.help, .toggle <module>, .bind <module> <key|mouse4|mmb|none> [hold], .binds, .macro add|del|list <key> <text>, .friend add|del|list|clear <nick>, .wp add <name> [x y z], .wp del <name>, .wp list, .config save|load|del <name>, .config list, .stats, .modules, .vclip <blocks>, .coords, .panic, .save

Modules
Combat: KillAura (priority, rotation, through walls, circle on target), TriggerBot, AimAssist, Criticals, Velocity, AutoArmor, AutoTotem
Movement: Sprint, Speed, Flight, ElytraFly, AirJump, Parkour, SafeWalk, Spider, NoFall, Step, Jesus, AutoWalk
Render: ESP, Nametags, ChestESP, BaseFinder, OreESP, Tracers, Breadcrumbs, Zoom, NoRender, Fullbright, Freecam, HoleESP, ItemESP, Waypoints, DamageNumbers, JumpCircles, Crosshair, Trajectories, FreeLook, Ambience, KillEffects, HealthBars, ProjectileTracker, BlockOutline, LightOverlay, Cosmetics, LogoutSpots
World: Nuker, Scaffold, FastPlace, AutoTool, AutoFish, AutoMLG, ChestStealer
Misc: AutoEat, AutoLog, MiddleClickFriend, Guardian, AutoReconnect, ChatPlus, HitSounds, Notifier, AntiAFK, AutoRespawn, DeathCoords
Client: HUD, GUI
New in 4.0
Keybinds from the menu — right-click any module: every module now has Bind, Mode and Show in list rows. Click Bind and press a key or a mouse button (middle / side buttons work); RMB clears, Esc cancels.
Toggle / Hold — Hold mode keeps a module on only while its key is held down.
Binds screen — the ⚙ Binds button in the dock lists every module with its key: click to rebind, switch Toggle / Hold, filter by name, show bound only, manage macros.
Reliable binds — keys and mouse buttons are handled as events, so even a very quick tap always toggles the module.
Favourites — hover a module and click ☆ to pin it to the top of its panel; search matches are highlighted.
Colour picker — hue / saturation / brightness sliders for colour settings and the Custom theme.
Macros — .macro add f6 /home or .macro add g gg: one key sends a chat message, a server command or a client command.
HealthBars — animated health bars with a trailing damage ghost above every mob.
ProjectileTracker — shows where every pearl, arrow, potion or trident already in the air will land, with time to impact; landings near you turn red.
BlockOutline — animated theme-coloured outline and fill around the block you look at.
LightOverlay — marks blocks where mobs can spawn: red at any time, yellow only at night.
Cosmetics — halo, orbiting aura and a coloured particle trail on your character (only you see them).
LogoutSpots — ghost box where a nearby player logged out, with how long ago and their health.
AutoMLG — water bucket clutch: places water right before you hit the ground and picks it back up.
ChatPlus — chat timestamps and bold highlighted mentions of your name with a sound.
HitSounds — a ding, bell, chime, click or arrow sound on every hit.
Performance widget — live FPS graph, server TPS, ping and a warning when the server stops responding. Inventory widget — your inventory always on screen.
New in 3.0
Guardian — threat warnings: a player is approaching, you’re being targeted with a bow/crossbow/trident, low HP (pulsating red vignette), armor is breaking, a totem has been triggered / there are no totems left, a creeper is hissing nearby.
DamageNumbers — pop‑up RPG numbers showing damage and healing over mobs and players.
KillEffects — lightning, blood, fireworks, or souls upon death + notification.
JumpCircles — gradient wave rings when jumping and landing.
Crosshair — your own crosshair (cross, circle, dot, T), expands while moving, hit marker, and reload bar.
Trajectories — the trajectory and fall point of arrows, pearls, tridents, snowballs, and potions.
Waypoints — marks in the world with a distance, a ray, and a ring; an automatic “Death” mark at the place of death.
FreeLook — rotate the camera around you while continuing to walk straight (hold down the assigned key).
Ambience — your own time of day and clear weather (only for you).
AutoReconnect — the “Reconnect” button on the disconnect screen, with the module — automatic reconnection after N seconds.
Profiles — .config save pvp / .config load pvp: modules, settings and HUD layout.
Friends are not attacked by combat modules and are highlighted with the theme color in ESP, Tracers, Nametags and on the radar. The config is stored in config/emperor.json, profiles — in config/emperor/profiles/.
