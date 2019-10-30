/*
 * Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.esri.arcgisruntime.toolkit.test.scalebar;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import com.esri.arcgisruntime.toolkit.scalebar.style.Style;
import com.esri.arcgisruntime.toolkit.test.R;

/**
 * Displays a dialog asking the user to select a scalebar style option.
 */
public final class ScalebarStyleDialogFragment extends DialogFragment {

  /**
   * The host activity must implement this interface to receive the callback.
   */
  public interface Listener {
    /**
     * Called when user selects a scalebar style option.
     *
     * @param style the selected style
     */
    void onScalebarStyleSpecified(Style style);
  }

  private Listener mListener;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    // Verify that the host activity implements the callback interface
    try {
      // Instantiate the Listener so we can send events to the host
      mListener = (Listener) context;
    } catch (ClassCastException e) {
      // The activity doesn't implement the interface, throw an exception
      throw new ClassCastException(context.toString() + " must implement ScalebarStyleDialogFragment.Listener");
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Scalebar style:")
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            // User cancelled the dialog - do nothing
          }
        })
        .setItems(R.array.scalebar_styles, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // Make callback with the selected item
            switch (which) {
              case 0:
                mListener.onScalebarStyleSpecified(Style.BAR);
                break;
              case 1:
                mListener.onScalebarStyleSpecified(Style.ALTERNATING_BAR);
                break;
              case 2:
                mListener.onScalebarStyleSpecified(Style.LINE);
                break;
              case 3:
                mListener.onScalebarStyleSpecified(Style.GRADUATED_LINE);
                break;
              case 4:
                mListener.onScalebarStyleSpecified(Style.DUAL_UNIT_LINE);
                break;
            }
          }
        });
    // Create the AlertDialog object and return it
    return builder.create();
  }
}
