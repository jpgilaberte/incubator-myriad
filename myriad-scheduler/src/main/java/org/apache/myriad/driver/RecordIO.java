package org.apache.myriad.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RecordIO {

    private BufferedReader br;

    public RecordIO(InputStream is){
        br = new BufferedReader(new InputStreamReader(is));
    }

    public String read() throws IOException {
        String res = br.readLine();
        int recordIoLength = new Integer(res);
        char[] recordIOBuffer = new char[recordIoLength];
        br.read(recordIOBuffer);
        return new String(recordIOBuffer);
    }
}
