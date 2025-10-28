package com.example.testing5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ShopBuyAdapter extends BaseAdapter {
    
    private Context context;
    private List<Skin> skins;
    private OnSkinClickListener listener;
    
    public interface OnSkinClickListener {
        void onSkinClick(Skin skin);
    }
    
    public ShopBuyAdapter(Context context, List<Skin> skins) {
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
        
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.skin_grid_item, parent, false);
            holder = new ViewHolder();
            holder.skinImage = convertView.findViewById(R.id.skinImage);
            holder.statusText = convertView.findViewById(R.id.statusText);
            holder.rarityIndicator = convertView.findViewById(R.id.rarityIndicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        Skin skin = skins.get(position);
        
        // Set skin image size (half the container size = 40dp)
        float density = context.getResources().getDisplayMetrics().density;
        int imageSize = (int) (40 * density);
        android.view.ViewGroup.LayoutParams imageParams = holder.skinImage.getLayoutParams();
        imageParams.width = imageSize;
        imageParams.height = imageSize;
        holder.skinImage.setLayoutParams(imageParams);
        
        // Set skin image
        if (skin.getImageBase64() != null && !skin.getImageBase64().isEmpty()) {
            SkinManager.getInstance().setImageFromBase64(holder.skinImage, skin.getImageBase64());
        }
        
        // Set status text to show price
        holder.statusText.setText(skin.getPrice() + " coins");
        holder.statusText.setVisibility(View.VISIBLE);
        
        // Show rarity indicator
        if (skin.getRarity() != null) {
            holder.rarityIndicator.setVisibility(View.VISIBLE);
            String rarityText = getRarityDisplayText(skin.getRarity());
            holder.rarityIndicator.setText(rarityText);
            
            // Set rarity color
            int rarityColor = getRarityColor(skin.getRarity());
            holder.rarityIndicator.setTextColor(rarityColor);
        } else {
            holder.rarityIndicator.setVisibility(View.GONE);
        }
        
        // Make image circular
        holder.skinImage.setClipToOutline(true);
        holder.skinImage.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        
        // Set click listener
        convertView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSkinClick(skin);
            }
        });
        
        return convertView;
    }
    
    private String getRarityDisplayText(Skin.Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return "C";
            case UNCOMMON:
                return "U";
            case RARE:
                return "R";
            default:
                return "";
        }
    }
    
    private int getRarityColor(Skin.Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return 0xFF808080; // Gray
            case UNCOMMON:
                return 0xFF00FF00; // Green
            case RARE:
                return 0xFFFFD700; // Gold
            default:
                return 0xFFFFFFFF; // White
        }
    }

    private static class ViewHolder {
        ImageView skinImage;
        TextView statusText;
        TextView rarityIndicator;
    }
}
