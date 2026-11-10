# ♛ Emperor Client

Fabric-клиент для Minecraft **1.21.4** (Java 21, Fabric API) в стиле Meteor:
анимированный ClickGUI, современный HUD, команды, друзья и 50+ модулей.

## Сборка

```bash
./gradlew build
# jar: build/libs/emperor-client-2.0.0.jar -> положить в .minecraft/mods вместе с Fabric API
```

## Управление

| Действие | Как |
|---|---|
| Открыть меню | **Right Shift** |
| Вкл/выкл модуль | ЛКМ |
| Настройки модуля | ПКМ |
| Назначить клавишу | СКМ, затем клавиша (Backspace — сбросить) |
| Поиск | просто начните печатать (Esc — очистить) |
| Режимы | ЛКМ — следующий, ПКМ — предыдущий |

## Интерфейс

- ClickGUI: размытие фона, частицы, градиентные заголовки, тени и свечение, плавное открытие и
  раскрытие настроек, анимированные переключатели и слайдеры, всплывающие подсказки, поиск с числом результатов.
- 10 тем (`Client → GUI → Theme`): Emperor (золото → королевский фиолетовый), Amethyst, Crimson, Ocean,
  Aurora, Sakura, Mint, Sunset, Midnight, Rainbow. Также настраиваются скорость градиента, скругление, прозрачность.
- HUD: водяной знак (FPS / пинг / время), анимированный список модулей, инфо-панель (XYZ, координаты Незера,
  скорость, направление), броня, Target HUD с головой и полоской здоровья, всплывающие уведомления.
- Брендинг на главном экране и в заголовке окна, иконка мода.

## Команды (префикс `.`)

`.help`, `.toggle <модуль>`, `.bind <модуль> <клавиша|none>`, `.friend add|del|list|clear <ник>`,
`.modules`, `.vclip <блоки>`, `.coords`, `.panic`, `.save`

## Модули

- **Combat:** KillAura (приоритет, вращение, через стены, круг на цели), TriggerBot, AimAssist, Criticals,
  Velocity, AutoArmor, AutoTotem
- **Movement:** Sprint, Speed, Flight, ElytraFly, AirJump, Parkour, SafeWalk, Spider, NoFall, Step, Jesus, AutoWalk
- **Render:** ESP, Nametags, ChestESP, BaseFinder, OreESP, Tracers, Breadcrumbs, Zoom (колесо — степень зума),
  NoRender, Fullbright, Freecam, HoleESP, ItemESP
- **World:** Nuker, Scaffold, FastPlace, AutoTool, AutoFish, ChestStealer
- **Misc:** AutoEat, AutoLog, MiddleClickFriend, Notifier, AntiAFK, AutoRespawn, DeathCoords
- **Client:** HUD, GUI

Друзья не атакуются боевыми модулями и подсвечиваются цветом темы в ESP, Tracers и Nametags.
Конфиг хранится в `config/emperor.json`.
