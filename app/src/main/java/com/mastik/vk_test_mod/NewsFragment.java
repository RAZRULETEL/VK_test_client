package com.mastik.vk_test_mod;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mastik.vk_test_mod.dataTypes.VKUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NewsFragment extends Fragment {

    public NewsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_news, container, false);

        RequestQueue queue = Volley.newRequestQueue(getContext());

        String url ="https://api.vk.com/method/newsfeed.get?access_token="+MainActivity.getToken()+"&count=15&v=5.131";//

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            VKUser[] groups = new VKUser[new JSONObject(response).getJSONObject("response").getJSONArray("groups").length()];
                            JSONArray resp_groups = new JSONObject(response).getJSONObject("response").getJSONArray("groups");
                            for(int i = 0; i < resp_groups.length(); i++)
                                groups[i] = VKUser.getFromJSON(resp_groups.getJSONObject(i));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.toString());
//                isError = true;
            }
        });

        queue.add(stringRequest);



        return root;
    }
}