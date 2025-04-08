# VeloBoard

Scoreboard API for Velocity plugins (1.18.2-1.21.5)

⚠️ After installing first read [Register Packets](#registering-packets)!

## Features

* Works from version 1.18.2 to 1.21.5
* Pretty small
* Easy to use
* Integrates [Adventure](https://github.com/KyoriPowered/adventure)
* Dynamic scoreboard size: no need to add/remove lines, as you can directly give a collection to change all the lines

## Getting started
Note: You can find the current version [here](https://repo.skyblocksquad.de/#/repo/de/timongcraft/VeloBoard).
(you can also find artifact snippets there)

<details>
  <summary style="font-size: 16px; font-weight: bold;">Gradle</summary>

  ```kotlin
  plugins {
      // version can be found here: https://plugins.gradle.org/plugin/com.gradleup.shadow
      id("com.gradleup.shadow") version "<version>"
  }

  repositories {
      maven {
          url = uri("https://repo.skyblocksquad.de/repo")
      }
  }

  dependencies {
      // version can be found here: https://repo.skyblocksquad.de/#/repo/de/timongcraft/VeloBoard
      implementation("de.timongcraft:VeloBoard:<version>")
  }

  shadowJar {
      // Replace 'com.yourpackage' with the package of your plugin 
      relocate("de.timongcraft.veloboard", "com.yourpackage.shadow.veloboard")
  }
  ```
</details>

<p>

<details>
  <summary style="font-size: 16px; font-weight: bold;">Maven</summary>

  ```xml
  <build>
      <plugins>
          <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-shade-plugin</artifactId>
              <version>3.6.0</version>
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
                          <pattern>de.timongcraft.veloboard</pattern>
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
          <groupId>de.timongcraft</groupId>
          <artifactId>VeloBoard</artifactId>
          <!-- version can be found here: https://repo.skyblocksquad.de/#/repo/de/timongcraft/VeloBoard ! -->
          <version>CURRENT_VERSION</version>
      </dependency>
  </dependencies>
  ```

Make sure to build directly with Maven and not with your IDE configuration (on IntelliJ IDEA: in the `Maven` tab on the right, in `Lifecycle`, use `package`).
</details>

## Usage

### Registering packets

Make sure to call `VeloBoardRegistry.register()` in the `ProxyInitializeEvent` to register the necessary packets.

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

<details>
  <summary>Small example plugin with a scoreboard that refreshes every second</summary>

  ```java
  package de.timongcraft.veloboard.example;

  import com.google.inject.Inject;
  import com.velocitypowered.api.event.Subscribe;
  import com.velocitypowered.api.event.connection.DisconnectEvent;
  import com.velocitypowered.api.event.connection.PostLoginEvent;
  import com.velocitypowered.api.event.player.ServerPostConnectEvent;
  import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
  import com.velocitypowered.api.plugin.Plugin;
  import com.velocitypowered.api.proxy.Player;
  import com.velocitypowered.api.proxy.ProxyServer;
  import de.timongcraft.veloboard.VeloBoard;
  import de.timongcraft.veloboard.VeloBoardRegistry;
  import net.kyori.adventure.text.Component;
  import net.kyori.adventure.text.format.NamedTextColor;

  import java.time.Duration;
  import java.util.HashMap;
  import java.util.Map;
  import java.util.UUID;

  @Plugin(
          id = "example",
          name = "ExamplePlugin",
          version = "1.0.0"
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
              for (VeloBoard board : boards.values()) {
                  updateBoard(board);
              }
          }).repeat(Duration.ofSeconds(1)).schedule();
      }

      @Subscribe
      public void onPostLogin(PostLoginEvent event) {
          Player player = event.getPlayer();
  
          VeloBoard board = new VeloBoard(player, Component.text("VeloBoard").color(NamedTextColor.BLUE));
          boards.put(player.getUniqueId(), board);
      }

      @Subscribe
      public void onServerPostConnect(ServerPostConnectEvent event) {
          boards.get(event.getPlayer().getUniqueId()).resend();
      }

      @Subscribe
      public void onDisconnect(DisconnectEvent event) {
          Player player = event.getPlayer();

          VeloBoard board = boards.remove(player.getUniqueId());
          if (board != null) {
              board.delete();
          }
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
</details>

-----

# Forked From/Based On

- [FastBoard](https://github.com/MrMicky-FR/FastBoard) (MIT License)
