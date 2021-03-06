package cn.nukkit.resourcepacks;

import cn.nukkit.Server;
import com.google.common.io.Files;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class ResourcePackManager {
    private final ResourcePack[] resourcePacks;
    private final ResourcePack[] behaviorPacks;
    private final Map<String, ResourcePack> allPacksById = new HashMap<>();
    private final Map<String, ResourcePack> resourcePacksById = new HashMap<>();
    private final Map<String, ResourcePack> behaviorPacksById = new HashMap<>();

    public ResourcePackManager(File path) {
        if (!path.exists()) {
            path.mkdirs();
        } else if (!path.isDirectory()) {
            throw new IllegalArgumentException(Server.getInstance().getLanguage()
                    .translateString("nukkit.resources.invalid-path", path.getName()));
        }

        List<ResourcePack> loadedResourcePacks = new ArrayList<>();
        List<ResourcePack> loadedBehaviorPacks = new ArrayList<>();
        for (File pack : path.listFiles()) {
            try {
                ResourcePack resourcePack = null;

                if (!pack.isDirectory()) { //directory resource packs temporarily unsupported
                    switch (Files.getFileExtension(pack.getName())) {
                        case "zip":
                        case "mcpack":
                            resourcePack = new ZippedResourcePack(pack);
                            break;
                        default:
                            log.warn(Server.getInstance().getLanguage()
                                    .translateString("nukkit.resources.unknown-format", pack.getName()));
                            break;
                    }
                }

                if (resourcePack != null) {
                    if (resourcePack.getPackType().equals("resources")) {
                        loadedResourcePacks.add(resourcePack);
                        this.resourcePacksById.put(resourcePack.getPackId(), resourcePack);
                        this.allPacksById.put(resourcePack.getPackId(), resourcePack);
                    }
                    else if (resourcePack.getPackType().equals("data")) {
                        loadedBehaviorPacks.add(resourcePack);
                        this.behaviorPacksById.put(resourcePack.getPackId(), resourcePack);
                        this.allPacksById.put(resourcePack.getPackId(), resourcePack);
                    } else {
                        log.warn(Server.getInstance().getLanguage()
                                .translateString("nukkit.resources.unknown-format", pack.getName()));
                    }
                }
            } catch (IllegalArgumentException e) {
                log.warn(Server.getInstance().getLanguage()
                        .translateString("nukkit.resources.fail", pack.getName(), e.getMessage()));
            }
        }

        this.resourcePacks = loadedResourcePacks.toArray(new ResourcePack[0]);
        this.behaviorPacks = loadedBehaviorPacks.toArray(new ResourcePack[0]);
        log.info(Server.getInstance().getLanguage()
                .translateString("nukkit.resources.success", String.valueOf(this.allPacksById.size())));
    }

    public Map<String, ResourcePack> getResourcePacksMap() {
        return resourcePacksById;
    }

    public Map<String, ResourcePack> getBehaviorPacksMap() {
        return behaviorPacksById;
    }

    public ResourcePack[] getResourceStack() {
        return this.resourcePacks;
    }

    public ResourcePack[] getBehaviorStack() {
        return this.behaviorPacks;
    }

    public ResourcePack getPackById(String id) {
        return this.allPacksById.get(id);
    }
}
