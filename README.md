♛ Emperor Client
Fabric client for Minecraft 1.21.4 (Java 21, Fabric API) in the Meteor style: animated ClickGUI, Spotlight search, HUD editor, 10 HUD widgets, commands, profiles, вейпоинты and 60+ modules.

YOU CAN GET IT HERE: https://github.com/sasabatorin2-collab/Emperor-Client/releases

Interface
ClickGUI: background blur, particles, gradient headers, shadows and glows, smooth opening and closing of settings, animated switches and sliders, click wave, smooth scrolling, hints.
Dock at the bottom of the menu: HUD Editor · Spotlight · one‑click theme change · reset panel layout.
Spotlight: macOS‑style window — fuzzy module search (Enter — on/off, Shift+Enter — open settings), and if you start with . — hints and command execution.
HUD Editor: dragging widgets with snapping to the edges and center of the screen, RMB — hide/show, LMB — reset, R — reset all. Positions are saved and adjusted to any resolution.
HUD widgets: watermark, module list, info (XYZ / Nether / speed / direction), armor with % durability, Target HUD, radar with a scanner beam, compass with waypoint markers, session statistics (time, kills, deaths, K/D, blocks, distance), keys with CPS, effects with icons and timers.
10 themes (Client → GUI → Theme): Emperor, Amethyst, Crimson, Ocean, Aurora, Sakura, Mint, Sunset, Midnight, Rainbow.
Commands (prefix .):
.help, .toggle <module>, .bind <module> <key|none>, .friend add|del|list|clear <nick>, .wp add <name> [x y z], .wp del <name>, .wp list, .config save|load|del <name>, .config list, .stats, .modules, .vclip <blocks>, .coords, .panic, .save

Modules
Combat: KillAura (priority, rotation, through walls, circle on target), TriggerBot, AimAssist, Criticals, Velocity, AutoArmor, AutoTotem
Movement: Sprint, Speed, Flight, ElytraFly, AirJump, Parkour, SafeWalk, Spider, NoFall, Step, Jesus, AutoWalk
Render: ESP, Nametags, ChestESP, BaseFinder, OreESP, Tracers, Breadcrumbs, Zoom, NoRender, Fullbright, Freecam, HoleESP, ItemESP, Waypoints, DamageNumbers, JumpCircles, Crosshair, Trajectories, FreeLook, Ambience, KillEffects
World: Nuker, Scaffold, FastPlace, AutoTool, AutoFish, ChestStealer
Misc: AutoEat, AutoLog, MiddleClickFriend, Guardian, AutoReconnect, Notifier, AntiAFK, AutoRespawn, DeathCoords
Client: HUD, GUI
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
