package org.onosproject.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.Test;

import java.util.*;

/**
 * Created by root on 12/2/17.
 */
public class CrudTest {
    @Test
    public void insert()  {
        /*MongoCredential credential = MongoCredential.createCredential("dxy", "sdwan", "dxy".toCharArray());
        ServerAddress serverAddress = new ServerAddress("172.19.200.41", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress, Arrays.asList(credential));
        MongoDatabase db = mongoClient.getDatabase("sdwan");
        MongoCollection<Document> list =  db.getCollection("device").withCodecRegistry();
        System.out.print(list);*/
       /* List<ServerAddress> addresses = new ArrayList<ServerAddress>();
        ServerAddress address1 = new ServerAddress("172.19.200.41" , 27017);
        ServerAddress address2 = new ServerAddress("172.19.200.41" , 27018);
        ServerAddress address3 = new ServerAddress("172.19.200.41" , 27019);
        addresses.add(address1);
        addresses.add(address2);
        addresses.add(address3);

        MongoClient client = new MongoClient(addresses);

        MongoDatabase mongoDatabase = client.getDatabase("mycol");
        MongoCollection<Document> collection = mongoDatabase.getCollection("test");
        System.out.println("集合 test 选择成功");

        Document document = new Document("title", "MongoDB").
                append("description", "database").
                append("likes", 100).
                append("by", "Fly");
        List<Document> documents = new ArrayList<Document>();
        documents.add(document);
        DefaultConfigVxlanTunnelInput input = new DefaultConfigVxlanTunnelInput();
        input.name("dd");
        Document document1 = new Document("ddd",input);
        collection.insertOne(document1);
        //collection.insertMany(documents);
        System.out.print("文档插入成功");*/
        Document document = new Document("title", "MongoDB").
                append("description", "database").
                append("likes", 100).
                append("by", "Fly");
        MongoCollection<Document> coll = MongoDBUtil.getDB("sdwan").getCollection("engipsec");
        // MongoDBUtil.insertOne(coll,"aa",document);


    }

    @Test
    public void find() {
        List<ServerAddress> addresses = new ArrayList<ServerAddress>();
        ServerAddress address1 = new ServerAddress("172.19.200.41" , 27017);
        ServerAddress address2 = new ServerAddress("172.19.200.41" , 27018);
        ServerAddress address3 = new ServerAddress("172.19.200.41" , 27019);
        addresses.add(address1);
        addresses.add(address2);
        addresses.add(address3);

        MongoClient client = new MongoClient(addresses);

        MongoDatabase mongoDatabase = client.getDatabase("mycol");
        MongoCollection<Document> collection = mongoDatabase.getCollection("test");
        System.out.println("集合 test 选择成功");

        FindIterable<Document> findIterable = collection.find();
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        while(mongoCursor.hasNext()){
            Document document = mongoCursor.next();
            System.out.println(document.getInteger("likes"));
            System.out.println(document);
        }
    }

    @Test
    public void update() {
        List<ServerAddress> addresses = new ArrayList<ServerAddress>();
        ServerAddress address1 = new ServerAddress("172.19.200.41" , 27017);
        ServerAddress address2 = new ServerAddress("172.19.200.41" , 27018);
        ServerAddress address3 = new ServerAddress("172.19.200.41" , 27019);
        addresses.add(address1);
        addresses.add(address2);
        addresses.add(address3);

        MongoClient client = new MongoClient(addresses);

        MongoDatabase mongoDatabase = client.getDatabase("mycol");
        MongoCollection<Document> collection = mongoDatabase.getCollection("test");
        System.out.println("集合 test 选择成功");

        //更新文档   将文档中likes=100的文档修改为likes=200
        collection.updateMany(Filters.eq("likes", 100), new Document("$set",new Document("likes",200)));
        //检索查看结果
        FindIterable<Document> findIterable = collection.find();
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        while(mongoCursor.hasNext()){
            System.out.println(mongoCursor.next());
        }
    }

    @Test
    public void delete()  {
        List<ServerAddress> addresses = new ArrayList<ServerAddress>();
        ServerAddress address1 = new ServerAddress("172.19.200.41" , 27017);
        ServerAddress address2 = new ServerAddress("172.19.200.41" , 27018);
        ServerAddress address3 = new ServerAddress("172.19.200.41" , 27019);
        addresses.add(address1);
        addresses.add(address2);
        addresses.add(address3);

        MongoClient client = new MongoClient(addresses);

        MongoDatabase mongoDatabase = client.getDatabase("mycol");
        MongoCollection<Document> collection = mongoDatabase.getCollection("test");
        System.out.println("集合 test 选择成功");


        //删除符合条件的第一个文档
        collection.deleteOne(Filters.and(Filters.eq("likes", 100),Filters.eq("title", "MongoDB")));
        //删除所有符合条件的文档
        collection.deleteMany (Filters.eq("likes", 200));
        //检索查看结果
        FindIterable<Document> findIterable = collection.find();
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        while(mongoCursor.hasNext()){
            System.out.println(mongoCursor.next());
        }
    }

    @Test
    public void testInt2Ip()  {
        MongoCredential credential = MongoCredential.createCredential("sdwan", "sdwan", "sdwan".toCharArray());
        ServerAddress serverAddress = new ServerAddress("172.19.200.41", 27017);
        MongoClient mongoClient = new MongoClient(serverAddress, Arrays.asList(credential));
        MongoDatabase mongoDatabase = mongoClient.getDatabase("sdwan");
        MongoCollection<Document> collection = mongoDatabase.getCollection("test");
        /*Document document = new Document("title", "MongoDB").
                append("description", "database").
                append("likes", 100).
                append("by", "Fly");
        collection.insertOne(document);*/

        String jsonData = "{'name':'Tom', 'grade':[{'course':'English','score':86},{'course':'Math','score':90}]}";

        //List<Student> list = GsonUtil.parseJsonArrayWithGson(jsonData,Student.class);
        Document document = Document.parse(jsonData);
        //collection.insertOne(document);
        /*Grade grade = new Grade();
        //grade.set
        Student stu = new Student();
        stu.setName("lins");

        //MongoDBUtil.insertOne(Constants.sdwan.DBNAME,"test","addd",document);

        Student student = MongoDBUtil.findByKey(Constants.sdwan.DBNAME, "test", "addd", Student.class);

        //System.out.print(str);
        System.out.print(student);*/
        //System.out.print(list);

        if(document instanceof Document) {
            System.out.println(document);
        }


    }
    @Test
    public void test2String() {
        Map<String,String> map = new HashMap<>();

        map.put("111","222");
        map.put("333","444");
        for (Map.Entry<String,String> entry : map.entrySet()) {
            map.put(entry.getKey(),"5555");
        }
        System.out.println(map);

    }

}
