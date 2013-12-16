package twig.nguyen.mustachify2.activities.fragments;

import twig.nguyen.mustachify2.R;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class ExampleFragment extends SherlockFragment {
	// These values are automatically retained
	private int buy = 0;
	private int sell = 0;

	// Views are destroyed each time the activity is rotated.
	// These are NOT automatically retained by the fragment.
	private Button btnBuy;
	private Button btnSell;
	private TextView tvLabel;



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
	}


	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_example, container, true);

        // Store handles to the controls
        btnBuy = (Button) view.findViewById(R.id.btnBuy);
        btnSell = (Button) view.findViewById(R.id.btnSell);
        tvLabel = (TextView) view.findViewById(R.id.tvLabel);

        // Event handlers
        btnBuy.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buy++;
				updateLabel();
			}
		});

        btnSell.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sell++;
				updateLabel();
			}
		});

        return view;
    }


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		updateLabel();
	}


	private void updateLabel() {
		StringBuilder sb = new StringBuilder();
		sb.append("Buy: ");
		sb.append(buy);
		sb.append(", sell: ");
		sb.append(sell);

		tvLabel.setText(sb.toString());
	}
}
