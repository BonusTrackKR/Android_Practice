package com.example.mission20;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static String rssUrl = "http://api.sbs.co.kr/xml/news/rss.jsp?pmDiv=entertainment";

    ProgressDialog progressDialog;
    Handler handler = new Handler();

    RecyclerView recyclerView;
    RSSNewsAdapter adapter;
    ArrayList<RSSNewsItem> newsItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new RSSNewsAdapter();
        recyclerView.setAdapter(adapter);

        newsItemList = new ArrayList<RSSNewsItem>();

        final EditText editText1 = findViewById(R.id.editText1);
        editText1.setText(rssUrl);

        Button showButton = findViewById(R.id.showButton);
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputStr = editText1.getText().toString();
                showRSS(inputStr);
            }
        });
    }

    private void showRSS(String urlStr) {
        try {
            progressDialog = ProgressDialog.show(this, "RSS Refresh", "RSS 정보 업데이트 중...", true, true);
        } catch (Exception ex) {
            Log.e(TAG, "Error", ex);
        }
    }

    class RefreshThread extends Thread {
        String urlStr;

        public RefreshThread(String str) {
            urlStr = str;
        }

        public void run() {
            try {
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();

                URL urlForHttp = new URL(urlStr);

                InputStream instream = getInputStreamUsingHTTP(urlForHttp);
                Document document = builder.parse(instream);
                int countItem = processDocument(document);
                Log.d(TAG, countItem + " news item processed.");

                handler.post(updateRSSRunnable);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public InputStream getInputStreamUsingHTTP(URL url) throws Exception {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            int resCode = connection.getResponseCode();
            Log.d(TAG, "Response Code : " + resCode);

            InputStream instream = connection.getInputStream();

            return instream;
    }

    private int processDocument(Document doc) {
        newsItemList.clear();

        Element docEle = doc.getDocumentElement();
        NodeList nodelist = docEle.getElementsByTagName("item");
        int count = 0;
        if ((nodelist != null) && (nodelist.getLength()>0)) {
            for (int i=0;i<nodelist.getLength();i++) {
                RSSNewsItem newsItem = dissectNode(nodelist, i);
                if (newsItem != null) {
                    newsItemList.add(newsItem);
                    count++;
                }
            }
        }

        return count;
    }

    private RSSNewsItem dissectNode(NodeList nodelist, int index) {
        RSSNewsItem newsItem = null;

        try {
            Element entry = (Element) nodelist.item(index);

            Element title = (Element) entry.getElementsByTagName("title").item(0);
            Element link = (Element) entry.getElementsByTagName("link").item(0);
            Element description = (Element) entry.getElementsByTagName("description").item(0);

            NodeList pubDataNode = entry.getElementsByTagName("pubDate");
            if (pubDataNode == null) {
                pubDataNode = entry.getElementsByTagName("dc:date");
            }
            Element pubDate = (Element) pubDataNode.item(0);

            Element author = (Element) entry.getElementsByTagName("author").item(0);
            Element category = (Element) entry.getElementsByTagName("category").item(0);

            String titleValue = null;
            if (title != null) {
                Node firstChild = title.getFirstChild();
                if (firstChild != null) {
                    titleValue = firstChild.getNodeValue();
                }
            }

            String linkValue = null;
            if (link != null) {
                Node firstChild = link.getFirstChild();
                if (firstChild != null) {
                    linkValue = firstChild.getNodeValue();
                }
            }

            String descriptionValue = null;
            if (description != null) {
                Node firstChild = description.getFirstChild();
                if (firstChild != null) {
                    descriptionValue = firstChild.getNodeValue();
                }
            }

            String pubDateValue = null;
            if (pubDate != null) {
                Node firstChild = pubDate.getFirstChild();
                if (firstChild != null) {
                    pubDateValue = firstChild.getNodeValue();
                }
            }

            String authorValue = null;
            if (author != null) {
                Node firstChild = author.getFirstChild();
                if (firstChild != null) {
                    authorValue = firstChild.getNodeValue();
                }
            }

            String categoryValue = null;
            if (category != null) {
                Node firstChild = category.getFirstChild();
                if (firstChild != null) {
                    categoryValue = firstChild.getNodeValue();
                }
            }

            Log.d(TAG, "item code : " + titleValue + ", " + linkValue + ", " + descriptionValue + ", " + pubDateValue + ", " + authorValue + ", " + categoryValue);

            newsItem = new RSSNewsItem(titleValue, linkValue, descriptionValue, pubDateValue, authorValue, categoryValue);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return newsItem;
    }

    Runnable updateRSSRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Resources res = getResources();
                Drawable rssIcon = res.getDrawable(R.drawable.rss_icon);
                for (int i=0; i< newsItemList.size(); i++) {
                    RSSNewsItem newsItem = newsItemList.get(i);
                    newsItem.setIcon(rssIcon);
                    adapter.addItem(newsItem);
                }

                adapter.notifyDataSetChanged();

                progressDialog.dismiss();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    };
}