package com.wang17.myphone.util;

import org.dtools.ini.BasicIniFile;
import org.dtools.ini.BasicIniSection;
import org.dtools.ini.IniFile;
import org.dtools.ini.IniFileReader;
import org.dtools.ini.IniFileWriter;
import org.dtools.ini.IniItem;
import org.dtools.ini.IniSection;

import java.io.File;
import java.io.IOException;

public class IniUtils {
    IniFile ini = new BasicIniFile(false);//不使用大小写敏感
    public void readContent(String pathName){
        File file = new File(pathName);
        IniFileReader reader = new IniFileReader(ini, file);
        try {
            reader.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取ini文件的所有Section
        for(int i=0;i<ini.getNumberOfSections();i++){
            IniSection sec = ini.getSection(i);
            //获取每个Section的Item
            System.out.println("---- " + sec.getName() + " ----");
            for(IniItem item : sec.getItems()){
                System.out.println(item.getName() + " = " + item.getValue());
            }
        }
    }

    public void writeContent(String pathName){
        File file = new File(pathName);
        //创建一个数据Section，在本例中Section名为 config
        IniSection dataSection = new BasicIniSection( "config" );
        ini.addSection( dataSection );

        //在上面的Section中添加Item，包括name、sex、age
        IniItem nameItem = new IniItem( "name" );
        nameItem.setValue("烟雨江南");
        dataSection.addItem( nameItem );

        IniItem ageItem = new IniItem( "age" );
        ageItem.setValue("999999");
        dataSection.addItem( ageItem );

        IniItem sexItem = new IniItem( "sex" );
        sexItem.setValue("男");
        dataSection.addItem( sexItem );


        //将数据写入到磁盘
        IniFileWriter writer=new IniFileWriter(ini, file);
        try {
            writer.write();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
