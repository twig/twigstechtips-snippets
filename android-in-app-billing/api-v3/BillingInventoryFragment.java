import twig.nguyen.common.services.billing.IabHelper;
import twig.nguyen.common.services.billing.IabResult;
import twig.nguyen.common.services.billing.Inventory;
import twig.nguyen.common.services.billing.Purchase;
import android.os.Bundle;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Helper fragment helps keep the billing madness out of MainActivity.
 *
 * @author twig
 */
public class BillingInventoryFragment extends SherlockFragment {
  // Helper billing object
  private IabHelper mHelper;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);

    initialiseBilling();
  }


  private void initialiseBilling() {
    if (mHelper != null) {
      return;
    }

    // Create the helper, passing it our context and the public key to verify signatures with
    mHelper = new IabHelper(getActivity(), G.getApplicationKey());

    // Enable debug logging (for a production application, you should set this to false).
    // mHelper.enableDebugLogging(true);

    // Start setup. This is asynchronous and the specified listener will be called once setup completes.
    mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
      @Override
      public void onIabSetupFinished(IabResult result) {
        // Have we been disposed of in the meantime? If so, quit.
        if (mHelper == null) {
          return;
        }

        // Something went wrong
        if (!result.isSuccess()) {
          Log.e(getActivity().getApplicationInfo().name, "Problem setting up in-app billing: " + result.getMessage());
          return;
        }

        // IAB is fully set up. Now, let's get an inventory of stuff we own.
        mHelper.queryInventoryAsync(iabInventoryListener());
      }
    });
  }


  /**
   * Listener that's called when we finish querying the items and subscriptions we own
   */
  private IabHelper.QueryInventoryFinishedListener iabInventoryListener() {
    return new IabHelper.QueryInventoryFinishedListener() {
      @Override
      public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
        // Have we been disposed of in the meantime? If so, quit.
        if (mHelper == null) {
          return;
        }

        // Something went wrong
        if (!result.isSuccess()) {
          return;
        }

        // Do your checks here...

        // Do we have the premium upgrade?
        Purchase purchasePro = inventory.getPurchase(G.SKU_PRO); // Where G.SKU_PRO is your product ID (eg. permanent.ad_removal)
        G.settings.isPro = (purchasePro != null && G.verifyDeveloperPayload(purchasePro));

        // After checking inventory, re-jig stuff which the user can access now
        // that we've determined what they've purchased
        G.initialiseStuff();
      }
    };
  }

  /**
   * Very important!
   */
  @Override
  public void onDestroy() {
    super.onDestroy();

    if (mHelper != null) {
      mHelper.dispose();
      mHelper = null;
    }
  }
}
