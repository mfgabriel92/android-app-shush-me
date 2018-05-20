package com.example.gabriel.shushme;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.places.PlaceBuffer;

public class PlaceListAdapter extends RecyclerView.Adapter<PlaceListAdapter.PlaceViewHolder> {

    private Context mContext;
    private PlaceBuffer mPlaces;

    PlaceListAdapter(Context context, PlaceBuffer places) {
        this.mContext = context;
        this.mPlaces = places;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_place_card, parent, false);

        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        String placeName = mPlaces.get(position).getName().toString();
        String placeAddress = mPlaces.get(position).getAddress().toString();

        holder.mTvName.setText(placeName);
        holder.mTvAddress.setText(placeAddress);
    }

    @Override
    public int getItemCount() {
        if (mPlaces == null) {
            return 0;
        }

        return mPlaces.getCount();
    }

    public void swapData(PlaceBuffer places) {
        mPlaces = places;

        if (mPlaces != null) {
            notifyDataSetChanged();
        }
    }

    class PlaceViewHolder extends RecyclerView.ViewHolder {

        TextView mTvName;
        TextView mTvAddress;

        PlaceViewHolder(View itemView) {
            super(itemView);
            mTvName = itemView.findViewById(R.id.tvName);
            mTvAddress = itemView.findViewById(R.id.tvAddress);
        }
    }
}
