# ♛ Emperor Client

Fabric-клиент для Minecraft **1.21.4** (Java 21, Fabric API) в стиле Meteor:
анимированный ClickGUI, Spotlight-поиск, редактор HUD, 10 HUD-виджетов, команды, профили, вейпоинты и 60+ модулей.

YOU CAN GET IT HERE: https://github.com/sasabatorin2-collab/Emperor-Client/releases

## Сборка

```bash
./gradlew build
# jar: build/libs/emperor-client-3.0.0.jar -> положить в .minecraft/mods вместе с Fabric API
```

## Управление

| Действие | Как |
|---|---|
| Открыть меню | **Right Shift** |
| Spotlight (быстрый поиск и команды) | **`** (клавиша Ё) |
| Вкл/выкл модуль | ЛКМ |
| Настройки модуля | ПКМ |
| Назначить клавишу | СКМ, затем клавиша (Backspace — сбросить) |
| Поиск в меню | просто начните печатать (Esc — очистить) |
| Режимы | ЛКМ — следующий, ПКМ — предыдущий |

Обе клавиши можно переназначить в `Настройки → Управление → Emperor Client`.

## Интерфейс

- **ClickGUI:** размытие фона, частицы, градиентные заголовки, тени и свечение, плавное открытие и
  раскрытие настроек, анимированные переключатели и слайдеры, волна от клика, плавная прокрутка, подсказки.
- **Док внизу меню:** HUD Editor · Spotlight · смена темы в один клик · сброс расположения панелей.
- **Spotlight:** окно в стиле macOS — нечёткий поиск модулей (Enter — вкл/выкл, Shift+Enter — открыть
  настройки), а если начать с `.` — подсказки и запуск команд.
- **HUD Editor:** перетаскивание виджетов с привязкой к краям и центру экрана, ПКМ — скрыть/показать,
  СКМ — сбросить, R — сбросить всё. Позиции сохраняются и подстраиваются под любое разрешение.
- **HUD-виджеты:** водяной знак, список модулей, инфо (XYZ / Незер / скорость / направление), броня с % прочности,
  Target HUD, **радар** с лучом-сканером, **компас** с метками вейпоинтов, **статистика сессии**
  (время, убийства, смерти, K/D, блоки, дистанция), **клавиши** с CPS, **эффекты** с иконками и таймерами.
- 10 тем (`Client → GUI → Theme`): Emperor, Amethyst, Crimson, Ocean, Aurora, Sakura, Mint, Sunset, Midnight, Rainbow.

## Команды (префикс `.`)

`.help`, `.toggle <модуль>`, `.bind <модуль> <клавиша|none>`, `.friend add|del|list|clear <ник>`,
`.wp add <имя> [x y z]`, `.wp del <имя>`, `.wp list`, `.config save|load|del <имя>`, `.config list`,
`.stats`, `.modules`, `.vclip <блоки>`, `.coords`, `.panic`, `.save`

## Модули

- **Combat:** KillAura (приоритет, вращение, через стены, круг на цели), TriggerBot, AimAssist, Criticals,
  Velocity, AutoArmor, AutoTotem
- **Movement:** Sprint, Speed, Flight, ElytraFly, AirJump, Parkour, SafeWalk, Spider, NoFall, Step, Jesus, AutoWalk
- **Render:** ESP, Nametags, ChestESP, BaseFinder, OreESP, Tracers, Breadcrumbs, Zoom, NoRender, Fullbright,
  Freecam, HoleESP, ItemESP, **Waypoints**, **DamageNumbers**, **JumpCircles**, **Crosshair**, **Trajectories**,
  **FreeLook**, **Ambience**, **KillEffects**
- **World:** Nuker, Scaffold, FastPlace, AutoTool, AutoFish, ChestStealer
- **Misc:** AutoEat, AutoLog, MiddleClickFriend, **Guardian**, **AutoReconnect**, Notifier, AntiAFK, AutoRespawn, DeathCoords
- **Client:** HUD, GUI

### Новое в 3.0

- **Guardian** — предупреждения об угрозах: игрок приближается, на тебя целятся из лука/арбалета/трезубца,
  мало HP (пульсирующая красная виньетка), броня ломается, сработал тотем / тотемов не осталось, рядом шипит крипер.
- **DamageNumbers** — всплывающие RPG-цифры урона и лечения над мобами и игроками.
- **KillEffects** — молния, кровь, фейерверк или души при убийстве + уведомление.
- **JumpCircles** — градиентные кольца-волны при прыжке и приземлении.
- **Crosshair** — свой прицел (крест, круг, точка, T), расширяется в движении, хит-маркер и полоска перезарядки.
- **Trajectories** — траектория и точка падения стрел, жемчуга, трезубца, снежков и зелий.
- **Waypoints** — метки в мире с дистанцией, лучом и кольцом; автоматическая метка «Death» на месте смерти.
- **FreeLook** — вращай камеру вокруг себя, продолжая идти прямо (зажми назначенную клавишу).
- **Ambience** — своё время суток и ясная погода (только у тебя).
- **AutoReconnect** — кнопка «Reconnect» на экране отключения, с модулем — автопереподключение через N секунд.
- **Профили** — `.config save pvp` / `.config load pvp`: модули, настройки и раскладка HUD.

Друзья не атакуются боевыми модулями и подсвечиваются цветом темы в ESP, Tracers, Nametags и на радаре.
Конфиг хранится в `config/emperor.json`, профили — в `config/emperor/profiles/`.
