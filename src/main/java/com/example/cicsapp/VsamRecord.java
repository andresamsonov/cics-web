package com.example.cicsapp;

public class VsamRecord {
    private String key;
    private String data1;
    private String data2;

    public VsamRecord(String key, String data1, String data2) {
        this.key = key;
        this.data1 = data1;
        this.data2 = data2;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getData1() { return data1; }
    public void setData1(String data1) { this.data1 = data1; }
    public String getData2() { return data2; }
    public void setData2(String data2) { this.data2 = data2; }
}
