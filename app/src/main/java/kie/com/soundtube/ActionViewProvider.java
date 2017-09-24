package kie.com.soundtube;

import android.content.Context;
import android.util.Log;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

/**
 * Created by kieChang on 2017/9/7.
 */

public class ActionViewProvider extends ActionProvider {
    Context context;

    public ActionViewProvider(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public View onCreateActionView() {
        Log.d("provider", "createView");
        return null;
    }

    @Override
    public View onCreateActionView(MenuItem forItem) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = new SearchView(context);
        Log.d("provider", "createView");
        return null;
    }
}
