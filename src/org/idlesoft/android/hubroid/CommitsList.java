package org.idlesoft.android.hubroid;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class CommitsList extends Activity {
	public CommitListAdapter m_commitListAdapter;
	public ArrayAdapter<String> m_branchesAdapter;
	public ArrayList<String> m_branches;
	public ProgressDialog m_progressDialog;
	private SharedPreferences m_prefs;
	private SharedPreferences.Editor m_editor;
	public String m_repo_owner;
	public String m_repo_name;
	public Intent m_intent;
	public int m_position;

	private Runnable threadProc_gatherCommits = new Runnable() {
		public void run() {
			try {
				JSONArray commitsJSON = Hubroid.make_api_request(new URL("http://github.com/api/v2/json/commits/list/"
																		+ URLEncoder.encode(m_repo_owner)
																		+ "/"
																		+ URLEncoder.encode(m_repo_name)
																		+ "/"
																		+ URLEncoder.encode(m_branches.get(m_position)))).getJSONArray("commits");
				Log.d("debug1",commitsJSON.toString());
				m_commitListAdapter = new CommitListAdapter(CommitsList.this, commitsJSON);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

			runOnUiThread(new Runnable() {
				public void run() {
					ListView commitList = (ListView)findViewById(R.id.lv_commits_list_list);
					commitList.setAdapter(m_commitListAdapter);
					m_progressDialog.dismiss();
				}
			});
		}
	};

	private OnItemSelectedListener m_onBranchSelect = new OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
		{
			m_position = position;
			m_progressDialog = ProgressDialog.show(CommitsList.this, "Please wait...", "Loading Repository's Commits...", true);
			Thread thread = new Thread(null, threadProc_gatherCommits);
			thread.start();
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}
	};

	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!menu.hasVisibleItems()) {
			menu.add(0, 1, 0, "Clear Preferences");
			menu.add(0, 2, 0, "Clear Cache");
		}
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			m_editor.clear().commit();
			Intent intent = new Intent(CommitsList.this, Hubroid.class);
			startActivity(intent);
        	return true;
		case 2:
			File root = Environment.getExternalStorageDirectory();
			if (root.canWrite()) {
				File hubroid = new File(root, "hubroid");
				if (!hubroid.exists() && !hubroid.isDirectory()) {
					return true;
				} else {
					hubroid.delete();
					return true;
				}
			}
		}
		return false;
	}

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.commits_list);

        m_prefs = getSharedPreferences(Hubroid.PREFS_NAME, 0);
        m_editor = m_prefs.edit();

        TextView title = (TextView) findViewById(R.id.tv_top_bar_title);
        title.setText("Recent Commits");

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	m_repo_name = extras.getString("repo_name");
        	m_repo_owner = extras.getString("username");

			try {
				JSONObject branchesJson = Hubroid.make_api_request(new URL("http://github.com/api/v2/json/repos/show/"
						+ URLEncoder.encode(m_repo_owner)
						+ "/"
						+ URLEncoder.encode(m_repo_name)
						+ "/branches")).getJSONObject("branches");
				m_branches = new ArrayList<String>(branchesJson.length());
				Iterator<String> keys = branchesJson.keys();
				while (keys.hasNext()) {
				String next_branch = keys.next();
				m_branches.add(next_branch);
				}
				
				m_branchesAdapter = new ArrayAdapter<String>(CommitsList.this, android.R.layout.simple_spinner_item, m_branches);
				m_branchesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

				Spinner branchesSpinner = (Spinner)findViewById(R.id.spn_commits_list_branch_select);
				branchesSpinner.setAdapter(m_branchesAdapter);
				branchesSpinner.setOnItemSelectedListener(m_onBranchSelect);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
    }
}