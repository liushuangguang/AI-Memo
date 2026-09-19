import com.mongodb.reactivestreams.client.MongoClients;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Local-only lossless snapshot before replacing the ephemeral debug database. */
class LocalDbSnapshot {
  public static void main(String[] args) throws Exception {
    Path file = Path.of(args[1]).toAbsolutePath();
    var json = JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
    try (var client = MongoClients.create("mongodb://127.0.0.1:27018")) {
      var db = client.getDatabase("note");
      long count = 0;
      if ("export".equals(args[0])) {
        try(var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
          for(String name : Flux.from(db.listCollectionNames()).collectList().block()) {
            for(Document doc : Flux.from(db.getCollection(name).find()).collectList().block()) {
              writer.write(new Document("collection", name).append("document", doc).toJson(json));
              writer.newLine(); count++;
            }
          }
        }
      } else if ("import".equals(args[0])) {
        try(var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          for(String line; (line=reader.readLine()) != null;) {
            Document record = Document.parse(line);
            Document doc = record.get("document", Document.class);
            var collection = db.getCollection(record.getString("collection"));
            Document existing = Mono.from(collection.find(new Document("_id", doc.get("_id"))).first()).block();
            if(existing == null) Mono.from(collection.insertOne(doc)).block();
            else if(!existing.toJson(json).equals(doc.toJson(json))) throw new IllegalStateException("Refusing to overwrite different existing record");
            Document verified = Mono.from(collection.find(new Document("_id", doc.get("_id"))).first()).block();
            if(verified == null || !verified.toJson(json).equals(doc.toJson(json))) throw new IllegalStateException("Restore verification failed");
            count++;
          }
        }
      } else throw new IllegalArgumentException("export or import required");
      System.out.println(args[0]+" verified records="+count);
    }
  }
}
