package com.example.testing5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class SkinGridAdapter extends BaseAdapter {
    private Context context;
    private List<Skin> skins;
    private String selectedSkinId;
    private String currentUserId;
    private OnSkinClickListener listener;

    public interface OnSkinClickListener {
        void onSkinClick(Skin skin);
    }

    public SkinGridAdapter(Context context, List<Skin> skins, String selectedSkinId, String currentUserId) {
        this.context = context;
        this.skins = skins;
        this.selectedSkinId = selectedSkinId;
        this.currentUserId = currentUserId;
    }

    public void setOnSkinClickListener(OnSkinClickListener listener) {
        this.listener = listener;
    }

    public void updateSelectedSkin(String selectedSkinId) {
        this.selectedSkinId = selectedSkinId;
        notifyDataSetChanged();
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
            holder.equippedIndicator = convertView.findViewById(R.id.equippedIndicator);
            holder.lockedIndicator = convertView.findViewById(R.id.lockedIndicator);
            holder.rarityIndicator = convertView.findViewById(R.id.rarityIndicator);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Skin skin = skins.get(position);
        
        // Set image size to half the container size (40dp for 80dp container) - circular
        final ViewHolder finalHolder = holder;
        float density = context.getResources().getDisplayMetrics().density;
        int imageSize = (int) (40 * density); // Convert 40dp to pixels
        android.view.ViewGroup.LayoutParams params = finalHolder.skinImage.getLayoutParams();
        params.width = imageSize;
        params.height = imageSize;
        finalHolder.skinImage.setLayoutParams(params);
        
        // Make the image circular
        finalHolder.skinImage.setClipToOutline(true);
        finalHolder.skinImage.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });
        
        // Set skin image
        if (skin.getImageBase64() != null && !skin.getImageBase64().isEmpty()) {
            SkinManager.getInstance().setImageFromBase64(holder.skinImage, skin.getImageBase64());
        } else {
            holder.skinImage.setImageResource(android.R.color.transparent);
        }

        // Check if skin is owned by current user
        boolean isOwned = skin.getCurrentOwner() != null && skin.getCurrentOwner().equals(currentUserId);
        
        // Show equipped indicator if this is the selected skin
        if (skin.getSkinId().equals(selectedSkinId)) {
            holder.equippedIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.equippedIndicator.setVisibility(View.GONE);
        }

        // Show locked indicator if not owned
        if (!isOwned) {
            holder.lockedIndicator.setVisibility(View.VISIBLE);
            holder.skinImage.setAlpha(0.5f); // Make it semi-transparent
        } else {
            holder.lockedIndicator.setVisibility(View.GONE);
            holder.skinImage.setAlpha(1.0f); // Full opacity
        }

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
        TextView equippedIndicator;
        TextView lockedIndicator;
        TextView rarityIndicator;
    }
}
