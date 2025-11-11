package com.example.cse476;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ClubVH> {

    interface OnClubClick {
        void onClubClick(Club club);
    }

    private final List<Club> all = new ArrayList<>();
    private final List<Club> visible = new ArrayList<>();
    private final OnClubClick onClick;
    private boolean filterStemOnly = false;

    ClubAdapter(OnClubClick onClick) {
        this.onClick = onClick;
    }

    void setData(List<Club> clubs) {
        all.clear();
        if (clubs != null) all.addAll(clubs);
        applyFilters("", filterStemOnly);
    }

    void applyFilters(String query, boolean stemOnly) {
        filterStemOnly = stemOnly;
        String q = query == null ? "" : query.trim().toLowerCase();

        visible.clear();
        for (Club c : all) {
            boolean matchesQuery =
                    q.isEmpty()
                            || (c.name != null && c.name.toLowerCase().contains(q))
                            || (c.slug != null && c.slug.toLowerCase().contains(q));

            boolean matchesStem = !stemOnly
                    || (c.slug != null && c.slug.toLowerCase().contains("stem"))
                    || (c.name != null && c.name.toLowerCase().contains("stem"))
                    || (c.description != null && c.description.toLowerCase().contains("stem"));

            if (matchesQuery && matchesStem) {
                visible.add(c);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClubVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_club, parent, false);
        return new ClubVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubVH h, int position) {
        Club c = visible.get(position);
        h.tvName.setText(c.name != null ? c.name : (c.slug != null ? c.slug : "Club"));

        // subtitle: take description, else website, else address
        String sub = "";
        if (c.description != null && !c.description.trim().isEmpty()) {
            sub = c.description.trim().replaceAll("\\s+", " ");
        } else if (c.website != null && !c.website.trim().isEmpty()) {
            sub = c.website.trim();
        } else if (c.address != null && !c.address.trim().isEmpty()) {
            sub = c.address.trim();
        }
        h.tvSubtitle.setText(sub);

        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClubClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    static class ClubVH extends RecyclerView.ViewHolder {
        TextView tvName, tvSubtitle;
        ClubVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }
}
