package com.example.testing5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ShopRetrieveAdapter extends BaseAdapter {
    
    private Context context;
    private List<Skin> skins;
    private OnSkinClickListener listener;
    
    public interface OnSkinClickListener {
        void onSkinClick(Skin skin);
    }
    
    public ShopRetrieveAdapter(Context context, List<Skin> skins) {
        this.context = context;
        this.skins = skins;
    }
    
    public void setOnSkinClickListener(OnSkinClickListener listener) {
        this.listener = listener;
    }
    
    @Override
    public int getCount() {
        return skins.size();
    }
    
    @Override
    public Object getItem(int position) {
        return skins.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        final View finalConvertView = convertView;
        
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.skin_grid_item, parent, false);
            holder = new ViewHolder();
            holder.skinImage = convertView.findViewById(R.id.skinImage);
            holder.statusText = convertView.findViewById(R.id.statusText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        Skin skin = skins.get(position);
        
        // Set skin image
        if (skin.getImageBase64() != null && !skin.getImageBase64().isEmpty()) {
            SkinManager.getInstance().setSkinToImageView(holder.skinImage, skin.getImageBase64());
        }
        
        // Set status text (showing price for retrieval)
        holder.statusText.setText("Price: " + skin.getPrice());
        holder.statusText.setVisibility(View.VISIBLE);
        
        // Set click listener
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSkinClick(skin);
                }
            }
        });
        
        return convertView;
    }
    
    private static class ViewHolder {
        ImageView skinImage;
        TextView statusText;
    }
}
