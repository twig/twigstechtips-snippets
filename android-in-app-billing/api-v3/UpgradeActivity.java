import twig.nguyen.common.services.billing.IabHelper;
import twig.nguyen.common.services.billing.IabResult;
import twig.nguyen.common.services.billing.Purchase;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

public class UpgradeActivity extends SherlockFragmentActivity {
  // Billing helper object
  private IabHelper mHelper;
  private boolean mBillingServiceReady;


  @Override
  protected void onCreate(Bundle savedInstance) {
    super.onCreate(savedInstance);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.activity_upgrade);

    // Initialise buy buttons
    Button btn = (Button) findViewById(R.id.btnUpgrade);
    btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onButtonUpgradeClicked();
      }
    });

    updateInventoryUI();
    initialiseBilling();
  }



  private void initialiseBilling() {
    if (mHelper != null) {
      return;
    }

    // Create the helper, passing it our context and the public key to verify signatures with
    mHelper = new IabHelper(this, G.getApplicationKey());

    mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
      @Override
      public void onIabSetupFinished(IabResult result) {
        // Have we been disposed of in the meantime? If so, quit.
        if (mHelper == null) {
          return;
        }

        if (!result.isSuccess()) {
          // Oh noes, there was a problem.
          complain("Problem setting up in-app billing: " + result.getMessage());
          return;
        }

        // IAB is fully set up.
        mBillingServiceReady = true;

        // Custom function to update UI reflecting their inventory
        updateInventoryUI();
      }
    });
  }


  // User clicked the "Upgrade to Premium" button.
  public void onButtonUpgradeClicked() {
    if (!mBillingServiceReady) {
      Toast.makeText(UpgradeActivity.this, "Purchase requires Google Play Store (billing) on your Android.", Toast.LENGTH_LONG).show();
      return;
    }

    String payload = generatePayloadForSKU(G.SKU_PRO); // This is based off your own implementation.
    mHelper.launchPurchaseFlow(UpgradeActivity.this, G.SKU_PRO, G.BILLING_REQUEST_CODE, mPurchaseFinishedListener, payload);
  }



  /**
   * When In-App billing is done, it'll return information via onActivityResult().
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (mHelper == null) {
      return;
    }

    // Pass on the activity result to the helper for handling
    if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
      // not handled, so handle it ourselves (here's where you'd
      // perform any handling of activity results not related to in-app
      // billing...
      super.onActivityResult(requestCode, resultCode, data);
    }
  }




  /**
   * Very important
   */
  @Override
  public void onDestroy() {
    super.onDestroy();

    if (mHelper != null) {
      mHelper.dispose();
      mHelper = null;
    }
  }



  // Callback for when a purchase is finished
  private IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
    @Override
    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
      // if we were disposed of in the meantime, quit.
      if (mHelper == null) {
        return;
      }

      // Don't complain if cancelling
      if (result.getResponse() == IabHelper.IABHELPER_USER_CANCELLED) {
        return;
      }

      if (!result.isSuccess()) {
        complain("Error purchasing: " + result.getMessage());
        return;
      }

      if (!G.verifyDeveloperPayload(purchase)) {
        complain("Error purchasing. Authenticity verification failed.");
        return;
      }

      // Purchase was success! Update accordingly
      if (purchase.getSku().equals(G.SKU_PRO)) {
        Toast.makeText(UpgradeActivity.this, "Thank you for upgrading!", Toast.LENGTH_LONG).show();

        G.settings.isPro = true;
        G.initialiseStuff();

        // Update the UI to reflect their latest purchase
        updateInventoryUI();
      }
      // Consume product immediately
      else if (purchase.getSku().equals(G.SKU_CONSUMABLE_BULLETS)) {
        mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
          @Override
          public void onConsumeFinished(Purchase purchase, IabResult result) {
            // if we were disposed of in the meantime, quit.
            if (mHelper == null) {
              return;
            }

            if (result.isSuccess()) {
              Toast.makeText(UpgradeActivity.this, "Bullet pack purchased", Toast.LENGTH_LONG).show();

              // Example of what you need to do
              playerBullets += 100;
            }
            else {
              complain("Error while consuming: " + result);
            }

            // Update the UI to reflect their latest purchase
            updateInventoryUI();
          }
        });
      }
    }
  };
}
