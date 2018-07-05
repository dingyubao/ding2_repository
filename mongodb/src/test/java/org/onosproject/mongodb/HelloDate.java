package org.onosproject.mongodb;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class HelloDate{
    public static void main(String[] args){
        Calendar cal = Calendar.getInstance();
        TimeZone timeZone = cal.getTimeZone();
        System.out.println(timeZone);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            Date x = sdf.parse("2014-05-08");
            writeObjectToFile(x);
            Object y = readObjectFromFile();

            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
            //sdf1.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
            String s = sdf1.format(x);
            System.out.println(s);
        }catch (ParseException e) {
            System.out.println(e.getMessage());
        }

    }

    public static void writeObjectToFile(Object obj)
    {
        File file =new File("test.dat");
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            ObjectOutputStream objOut=new ObjectOutputStream(out);
            objOut.writeObject(obj);
            objOut.flush();
            objOut.close();
            System.out.println("write object success!");
        } catch (IOException e) {
            System.out.println("write object failed");
            e.printStackTrace();
        }
    }

    public static Object readObjectFromFile()
    {
        Object temp=null;
        File file =new File("test.dat");
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            ObjectInputStream objIn=new ObjectInputStream(in);
            temp=objIn.readObject();
            objIn.close();
            System.out.println("read object success!");
        } catch (IOException e) {
            System.out.println("read object failed");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return temp;
    }
}