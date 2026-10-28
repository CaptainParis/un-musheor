package musheor.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;

public class PearlStore {
   private static final Path STORE_PATH = Path.of("meteor-client/pearls.json");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final List<PearlStore.PearlRecord> pearls = new ArrayList<>();

   public static List<PearlStore.PearlRecord> getAll() {
      return pearls;
   }

   public static void addOrUpdate(String ownerName, BlockPos trapdoorPos, int entityId) {
      for (PearlStore.PearlRecord r : pearls) {
         if (r.trapdoorPos.equals(trapdoorPos)) {
            r.ownerName = ownerName;
            r.entityId = entityId;
            save();
            return;
         }
      }

      pearls.add(new PearlStore.PearlRecord(ownerName, trapdoorPos, entityId));
      save();
   }

   public static void updateEntityId(BlockPos trapdoorPos, int entityId) {
      for (PearlStore.PearlRecord r : pearls) {
         if (r.trapdoorPos.equals(trapdoorPos)) {
            r.entityId = entityId;
            return;
         }
      }
   }

   public static PearlStore.PearlRecord findByTrapdoor(BlockPos trapdoorPos) {
      for (PearlStore.PearlRecord r : pearls) {
         if (r.trapdoorPos.equals(trapdoorPos)) {
            return r;
         }
      }

      return null;
   }

   public static void removeByOwner(String playerName) {
      pearls.removeIf(r -> r.ownerName.equalsIgnoreCase(playerName));
      save();
   }

   public static void clear() {
      pearls.clear();
      save();
   }

   public static List<PearlStore.PearlRecord> findByOwner(String ownerName) {
      List<PearlStore.PearlRecord> result = new ArrayList<>();

      for (PearlStore.PearlRecord r : pearls) {
         if (r.ownerName.equalsIgnoreCase(ownerName)) {
            result.add(r);
         }
      }

      return result;
   }

   public static void save() {
      try {
         Files.createDirectories(STORE_PATH.getParent());
         JsonArray arr = new JsonArray();

         for (PearlStore.PearlRecord r : pearls) {
            JsonObject obj = new JsonObject();
            obj.addProperty("owner", r.ownerName);
            obj.addProperty("entityId", r.entityId);
            obj.addProperty("x", r.trapdoorPos.getX());
            obj.addProperty("y", r.trapdoorPos.getY());
            obj.addProperty("z", r.trapdoorPos.getZ());
            arr.add(obj);
         }

         Files.writeString(STORE_PATH, GSON.toJson(arr));
      } catch (IOException var4) {
         var4.printStackTrace();
      }
   }

   public static void load() {
      pearls.clear();
      if (Files.exists(STORE_PATH)) {
         try {
            String json = Files.readString(STORE_PATH);

            for (JsonElement el : JsonParser.parseString(json).getAsJsonArray()) {
               JsonObject obj = el.getAsJsonObject();
               String owner = obj.get("owner").getAsString();
               int entityId = obj.get("entityId").getAsInt();
               int x = obj.get("x").getAsInt();
               int y = obj.get("y").getAsInt();
               int z = obj.get("z").getAsInt();
               pearls.add(new PearlStore.PearlRecord(owner, new BlockPos(x, y, z), entityId));
            }
         } catch (Exception var10) {
            var10.printStackTrace();
         }
      }
   }

   public static class PearlRecord {
      public String ownerName;
      public BlockPos trapdoorPos;
      public int entityId;

      public PearlRecord(String ownerName, BlockPos trapdoorPos, int entityId) {
         this.ownerName = ownerName;
         this.trapdoorPos = trapdoorPos;
         this.entityId = entityId;
      }
   }
}
