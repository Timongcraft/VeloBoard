# VeloBoard

Scoreboard API for Velocity plugins (1.18.2-1.20.4)

⚠️ After installing first read [Register Packets](#registering-packets)!

## Features

* Works from version 1.18.2 to 1.20.4
* Pretty small
* Easy to use
* Integrates [Adventure](https://github.com/KyoriPowered/adventure)
* Dynamic scoreboard size: you don't need to add/remove lines, you can directly give a string list (or array) to change all the lines
* Can be used asynchronously

## Getting started
Note: You can find the current version [here](https://repo.skyblocksquad.de/#/repo/timongcraft/veloboard).

### Maven

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.2</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>timongcraft.veloboard</pattern>
                        <!-- Replace 'com.yourpackage' with the package of your plugin ! -->
                        <shadedPattern>com.yourpackage.veloboard</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </plugin>
    </plugins>
</build>

<repositories>
    <repository>
        <id>skyblocksquad</id>
        <url>https://repo.skyblocksquad.de/repo<repository></url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>timongcraft.veloboard</groupId>
        <artifactId>VeloBoard</artifactId>
        <version>CURRENT_VERSION</version>
    </dependency>
</dependencies>
```

When using Maven, make sure to build directly with Maven and not with your IDE configuration (on IntelliJ IDEA: in the `Maven` tab on the right, in `Lifecycle`, use `package`).

### Gradle

```groovy
plugins {
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

repositories {
    maven {
        url "https://repo.skyblocksquad.de/repo"
    }
}

dependencies {
    implementation 'timongcraft:veloboard:CURRENT_VERSION'
}

shadowJar {
    // Replace 'com.yourpackage' with the package of your plugin 
    relocate 'timongcraft.veloboard', 'com.yourpackage.veloboard'
}
```

### Manual

Copy all files in your plugin.

## Usage

### Registering packets

First you need to call `VeloBoardRegistry.register()` in your `Main` or `ProxyInitializeEvent` to register the necessary packets.

### Creating a scoreboard

Simply create a new `VeloBoard` and update the title and the lines:

```java
VeloBoard board = new VeloBoard(player);

// Set the title
board.updateTitle(Component.text("VeloBoard").color(NamedTextColor.BLUE));

// Change the lines
board.updateLines(
        Component.empty(), // Empty line
        Component.text("One line"),
        Component.empty(),
        Component.text("Second line")
);
```

### Example

Small example plugin with a scoreboard that refreshes every second:

```java
package timongcraft.veloboard.example;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Plugin(
        id = "example",
        name = "ExamplePlugin",
        version = "1.0"
)
public class ExamplePlugin {

    private final ProxyServer server;
    private final Map<UUID, VeloBoard> boards = new HashMap<>();

    @Inject
    public ExamplePlugin(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        VeloBoardRegistry.register();

        server.getScheduler().buildTask(this, () -> {
            for (VeloBoard board : boards.values())
                updateBoard(board);
        }).repeat(Duration.ofSeconds(1)).schedule();
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        VeloBoard board = new VeloBoard(player);
        board.updateTitle(Component.text("VeloBoard").color(NamedTextColor.BLUE));
        boards.put(player.getUniqueId(), board);
    }

    @Subscribe
    @SuppressWarnings("UnstableApiUsage")
    public void onServerPostConnect(ServerPostConnectEvent event) {
        boards.get(event.getPlayer().getUniqueId()).resend();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();

        VeloBoard board = boards.remove(player.getUniqueId());
        if (board != null)
            board.delete();
    }

    private void updateBoard(VeloBoard board) {
        board.updateLines(
                Component.empty(),
                Component.text("Players: " + server.getPlayerCount()),
                Component.empty(),
                Component.text("Ping: " + board.getPlayer().getPing()),
                Component.empty()
        );
    }

}
```

-----

# Inspiration/Base

- [FastBoard](https://github.com/MrMicky-FR/FastBoard) (MIT License)

