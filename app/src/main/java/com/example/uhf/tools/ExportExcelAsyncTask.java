package com.example.uhf.tools;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;

import com.example.uhf.fragment.UHFReadTagFragment;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ExportExcelAsyncTask extends AsyncTask<String, Integer, Boolean> {

    protected ProgressDialog mypDialog;
    protected Activity mContext;

    String pathRoot = Environment.getExternalStorageDirectory() + File.separator + "UHF_exportData";
    String path = pathRoot + File.separator + GetTimesyyyymmddhhmmss() + ".xlsx";

    boolean isSotp = false;
    ArrayList<HashMap<String, String>> tagList;

    public ExportExcelAsyncTask(Activity act, ArrayList<HashMap<String, String>> tagList) {
        this.mContext = act;
        this.tagList = tagList;
        File file = new File(pathRoot);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        File file = new File(path);
        String[] head = new String[]{"EPC", "Count"};
        ExcelUtils excelUtils = new ExcelUtils();
        excelUtils.createExcel(file, head);

        List<String[]> list = new ArrayList<>();
        StringBuilder message = new StringBuilder();  // Will hold the entire comma-separated tag list

        // Loop through each tag in tagList
        for (int i = 0; !isSotp && i < tagList.size(); i++) {
            int progress = (int) (div(i + 1, tagList.size(), 2) * 100);
            publishProgress(progress);

            // Append EPC tag to message, followed by a comma except for the last tag
            message.append(tagList.get(i).get(UHFReadTagFragment.TAG_EPC_TID));
            if (i != tagList.size() - 1) {
                message.append(",");  // Add a comma if it's not the last element
            }

            // Add the EPC and count to the Excel list
            String[] data = new String[]{
                    tagList.get(i).get(UHFReadTagFragment.TAG_EPC_TID).replace("\n", "\r\n"),
                    tagList.get(i).get(UHFReadTagFragment.TAG_COUNT)
            };
            list.add(data);
        }

        // Send the entire concatenated EPC tag list as a single message to the API
        new SendMessageTask().execute(message.toString());

        // Add some additional rows in Excel for summary
        list.add(new String[]{"", ""});
        list.add(new String[]{"", ""});
        list.add(new String[]{"标签总数", String.valueOf(this.tagList.size())});

        long begin = System.currentTimeMillis();
        publishProgress(101);
        excelUtils.writeToExcel(list);  // Write all data to Excel file
        long waitTime = 6000 - (System.currentTimeMillis() - begin);
        sleepTime(waitTime);
        return true;
    }

    // AsyncTask to handle sending the message to the API
    private static class SendMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... messages) {
            for (String message : messages) {
                try {
                    sendMessage(message);  // Send the message to the API
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    // Function to send the message via HTTP POST request
    private static void sendMessage(String message) throws IOException {
        String urlString = "http://192.168.1.19:5000/api/data";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Prepare the JSON payload
        String payload = "{\"data\": \"" + message + "\"}";

        // Write the payload to the output stream
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int statusCode = connection.getResponseCode();
        if (statusCode != 200) {
            throw new IOException("Failed to send message. Status code: " + statusCode);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        mypDialog.cancel();  // Dismiss the progress dialog
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (values[0] == 101) {
            mypDialog.setMessage("path:" + path);
        } else {
            mypDialog.setProgress(values[0]);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mypDialog = new ProgressDialog(mContext);
        mypDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mypDialog.setMessage("Exporting...");
        mypDialog.setCanceledOnTouchOutside(false);
        mypDialog.setMax(100);
        mypDialog.setProgress(0);

        // Allow the dialog to be canceled by the user
        mypDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                isSotp = true;  // Stop the task if the dialog is canceled
            }
        });

        mypDialog.show();
    }

    // Helper function to get the current timestamp in yyyyMMddHHmmss format
    private String GetTimesyyyymmddhhmmss() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate);
    }

    // Sleep for a certain time (used after writing to Excel)
    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Precise division method
    private float div(float v1, float v2, int scale) {
        BigDecimal b1 = new BigDecimal(Float.toString(v1));
        BigDecimal b2 = new BigDecimal(Float.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).floatValue();
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        super.onCancelled(aBoolean);
        mContext = null;
        tagList = null;
    }
}
