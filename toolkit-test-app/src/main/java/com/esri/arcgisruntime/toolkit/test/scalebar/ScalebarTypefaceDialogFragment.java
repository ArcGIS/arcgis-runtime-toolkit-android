/*
 * Copyright 2017 Esri
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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import com.esri.arcgisruntime.toolkit.test.R;

/**
 * Displays a dialog asking the user to select a scalebar typeface option.
 */
public final class ScalebarTypefaceDialogFragment extends DialogFragment {

  /**
   * The host activity must implement this interface to receive the callback.
   */
  public interface Listener {
    /**
     * Called when user selects a scalebar typeface option.
     *
     * @param typeface the selected typeface
     * @since 100.1.0
     */
    void onScalebarTypefaceSpecified(Typeface typeface);
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
      throw new ClassCastException(context.toString() + " must implement ScalebarTypefaceDialogFragment.Listener");
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Scalebar typeface:")
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            // User cancelled the dialog - do nothing
          }
        })
        .setItems(R.array.scalebar_typefaces, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // Make callback with the selected item
            switch (which) {
              case 0:
                mListener.onScalebarTypefaceSpecified(Typeface.MONOSPACE);
                break;
              case 1:
                mListener.onScalebarTypefaceSpecified(Typeface.SANS_SERIF);
                break;
              case 2:
                mListener.onScalebarTypefaceSpecified(Typeface.SERIF);
                break;
              case 3:
                mListener.onScalebarTypefaceSpecified(Typeface.DEFAULT_BOLD);
                break;
              case 4:
                mListener.onScalebarTypefaceSpecified(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
                break;
            }
          }
        });
    // Create the AlertDialog object and return it
    return builder.create();
  }
}
