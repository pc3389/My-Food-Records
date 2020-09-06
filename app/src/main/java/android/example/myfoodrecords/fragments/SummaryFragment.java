package android.example.myfoodrecords.fragments;

import android.example.myfoodrecords.R;
import android.example.myfoodrecords.adapter.SummaryAdapter;
import android.example.myfoodrecords.model.Food;
import android.example.myfoodrecords.model.SummaryItem;
import android.example.myfoodrecords.utils.RealmHelper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;

public class SummaryFragment extends Fragment {

    private Realm realm;
    private RealmHelper helper;
    private RealmChangeListener realmChangeListener;

    private View rootView;

    private RecyclerView recyclerView;
    private Button foodButton;
    private Button placeButton;
    private Button typeButton;

    public static final String FOOD_STRING = "food";
    public static final String PLACE_STRING = "place";
    public static final String TYPE_STRING = "type";

    private static final String KEY_INDICATOR = "keyIndicator";
    private static int indicator = 0;

    private List<SummaryItem> summaryItemList;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupRealm();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_summary, container, false);
        setupUi();
        return rootView;
    }

    private void setupRealm() {
        realm = Realm.getDefaultInstance();
        helper = new RealmHelper(realm);
    }

    private void setupUi() {
        recyclerView = rootView.findViewById(R.id.summary_rc);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));

        updateUi();

        foodButton = rootView.findViewById(R.id.summary_food_button);
        foodButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indicator = 0;
                setFoodSummaryItemList();
                SummaryAdapter summaryAdapter = new SummaryAdapter(summaryItemList, getActivity());
                recyclerView.setAdapter(summaryAdapter);
                SummaryAdapter.foodOrPlace = FOOD_STRING;
            }
        });
        placeButton = rootView.findViewById(R.id.summary_place_button);
        placeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indicator = 1;
                setPlaceSummaryItemList();
                SummaryAdapter summaryAdapter = new SummaryAdapter(summaryItemList, getActivity());
                recyclerView.setAdapter(summaryAdapter);
                SummaryAdapter.foodOrPlace = PLACE_STRING;
            }
        });
        typeButton = rootView.findViewById(R.id.summary_type_button);
        typeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                indicator = 2;
                setTypeSummaryItemList();
                SummaryAdapter summaryAdapter = new SummaryAdapter(summaryItemList, getActivity());
                recyclerView.setAdapter(summaryAdapter);
                SummaryAdapter.foodOrPlace = TYPE_STRING;
            }
        });
        refresh();
    }

    private void updateUi() {
        if (indicator == 2) {
            setTypeSummaryItemList();
        } else if (indicator == 1) {
            setPlaceSummaryItemList();
        } else {
            setFoodSummaryItemList();
        }
        SummaryAdapter summaryAdapter = new SummaryAdapter(summaryItemList, getActivity());
        recyclerView.setAdapter(summaryAdapter);
    }

    private void setFoodSummaryItemList() {
        summaryItemList = new ArrayList<>();
        List<Food> sortedFoodList = helper.retrieveFoodWithNameSorted();
        if (sortedFoodList.size() > 0) {
            int count = 0;
            float rating = 0;

            for (int i = 0; i < sortedFoodList.size() - 1; i++) {
                rating += sortedFoodList.get(i).getRating();
                count++;
                if (!sortedFoodList.get(i).getName().equals(sortedFoodList.get(i + 1).getName())) {

                    String name = sortedFoodList.get(i).getName();
                    addItemToSummayItemList(name, count, rating);
                    count = 0;
                    rating = 0;
                }
            }
            if (sortedFoodList.size() == 1) {
                int lastIndex = sortedFoodList.size() - 1;
                String lastItemName = sortedFoodList.get(lastIndex).getName();
                float lastItemRating = sortedFoodList.get(lastIndex).getRating();
                addItemToSummayItemList(lastItemName, 1, lastItemRating);

            } else if (sortedFoodList.size() > 1) {
                int lastIndex = sortedFoodList.size() - 1;
                String lastItemName = sortedFoodList.get(lastIndex).getName();
                String secondLastName = sortedFoodList.get(lastIndex - 1).getName();
                float lastItemRating = sortedFoodList.get(lastIndex).getRating();

                if (!lastItemName.equals(secondLastName)) {
                    addItemToSummayItemList(lastItemName, 1, lastItemRating);
                } else {
                    addItemToSummayItemList(lastItemName, count + 1, rating + lastItemRating);
                }
            }
        }

    }

    private void setPlaceSummaryItemList() {
        summaryItemList = new ArrayList<>();
        List<Food> sortedFoodList = helper.retrieveFoodWithNameSorted();
        if (sortedFoodList.size() > 0) {
            List<Integer> removeNumber = new ArrayList<>();
            for (int i = 0; i < sortedFoodList.size(); i++) {
                if (sortedFoodList.get(i).getPlaceModel() == null) {
                    removeNumber.add(i);
                } else if (sortedFoodList.get(i).getPlaceModel().getPlaceName() == null) {
                    removeNumber.add(i);
                }
            }
            for (int i = removeNumber.size() - 1; i >= 0; i--) {
                sortedFoodList.remove((int) removeNumber.get(i));
            }
            Collections.sort(sortedFoodList, new FoodPlaceComparator());
            int count = 0;
            float rating = 0;

            for (int i = 0; i < sortedFoodList.size() - 1; i++) {
                rating += sortedFoodList.get(i).getRating();
                count++;
                String thisItemPlaceName = sortedFoodList.get(i).getPlaceModel().getPlaceName();
                String nextItemPlaceName = sortedFoodList.get(i + 1).getPlaceModel().getPlaceName();
                if (!thisItemPlaceName.equals(nextItemPlaceName)) {
                    String name = sortedFoodList.get(i).getPlaceModel().getPlaceName();
                    addItemToSummayItemList(name, count, rating);
                    count = 0;
                    rating = 0;
                }
            }

            if (sortedFoodList.size() == 1) {
                int lastIndex = sortedFoodList.size() - 1;
                String lastItemName = sortedFoodList.get(lastIndex).getPlaceModel().getPlaceName();
                float lastItemRating = sortedFoodList.get(lastIndex).getRating();
                addItemToSummayItemList(lastItemName, 1, lastItemRating);
            } else if (sortedFoodList.size() > 1) {
                int lastIndex = sortedFoodList.size() - 1;
                String lastItemName = sortedFoodList.get(lastIndex).getPlaceModel().getPlaceName();
                String secondLastName = sortedFoodList.get(lastIndex - 1).getPlaceModel().getPlaceName();
                float lastItemRating = sortedFoodList.get(lastIndex).getRating();

                if (!lastItemName.equals(secondLastName)) {
                    addItemToSummayItemList(lastItemName, 1, lastItemRating);
                } else {
                    addItemToSummayItemList(lastItemName, count + 1, rating + lastItemRating);
                }
            }
        }
    }

    private void setTypeSummaryItemList() {
        summaryItemList = new ArrayList<>();
        List<Food> sortedFoodList = helper.retrieveFoodWithTypeSorted();
        if (sortedFoodList.size() > 0) {
            int count = 0;
            float rating = 0;

            for (int i = 0; i < sortedFoodList.size() - 1; i++) {
                rating += sortedFoodList.get(i).getRating();
                count++;
                if (!sortedFoodList.get(i).getFoodType().equals(sortedFoodList.get(i + 1).getFoodType())) {

                    String foodType = sortedFoodList.get(i).getFoodType();
                    addItemToSummayItemList(foodType, count, rating);
                    count = 0;
                    rating = 0;
                }
            }
            if (sortedFoodList.size() == 1) {
                int lastIndex = sortedFoodList.size() - 1;
                String lastItemType = sortedFoodList.get(lastIndex).getFoodType();
                float lastItemRating = sortedFoodList.get(lastIndex).getRating();
                addItemToSummayItemList(lastItemType, 1, lastItemRating);

            } else if (sortedFoodList.size() > 1) {
                int lastIndex = sortedFoodList.size() - 1;
                String lastItemType = sortedFoodList.get(lastIndex).getFoodType();
                String secondLastType = sortedFoodList.get(lastIndex - 1).getFoodType();
                float lastItemRating = sortedFoodList.get(lastIndex).getRating();

                if (!lastItemType.equals(secondLastType)) {
                    addItemToSummayItemList(lastItemType, 1, lastItemRating);
                } else {
                    addItemToSummayItemList(lastItemType, count + 1, rating + lastItemRating);
                }
            }
        }

    }

    private static class FoodPlaceComparator implements Comparator<Food> {
        @Override
        public int compare(Food o1, Food o2) {
            return o1.getPlaceModel().getPlaceName().compareTo(o2.getPlaceModel().getPlaceName());
        }
    }

    private void addItemToSummayItemList(String name, int count, float rating) {
        SummaryItem summaryItem = new SummaryItem();
        summaryItem.setName(name);
        summaryItem.setCount(count);
        summaryItem.setRating(rating / count);
        summaryItemList.add(summaryItem);
    }

    private void refresh() {
        realmChangeListener = new RealmChangeListener() {
            @Override
            public void onChange(Object o) {
                setFoodSummaryItemList();
                SummaryAdapter summaryAdapter = new SummaryAdapter(summaryItemList, getActivity());
                recyclerView.setAdapter(summaryAdapter);
                SummaryAdapter.foodOrPlace = FOOD_STRING;
            }
        };
        realm.addChangeListener(realmChangeListener);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_INDICATOR, indicator);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            indicator = savedInstanceState.getInt(KEY_INDICATOR);
            updateUi();
        }
    }

}

