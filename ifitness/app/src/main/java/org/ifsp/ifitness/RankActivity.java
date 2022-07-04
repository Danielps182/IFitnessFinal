package org.ifsp.ifitness;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class RankActivity extends AppCompatActivity implements OnGetDataListener{
    private Toolbar toolbar;
    private LinearLayout first, second, third;
    private TextView totalAmountTxtview, firstPlace, secondPlace, thirdPlace, bestPlayers, allPlayersRanking;
    private ListView scoreList;
    private ArrayAdapter adapter;


    private LinkedHashSet<String> playersId;
    private int index, totalScore;
    private String[] scores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);
        initializeUI();
        getTotalAmountFromAllPlayers();
    }

    private void initializeUI(){
        toolbar  = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Distância Tracker");
        getSupportActionBar().getThemedContext();
        toolbar.setTitleTextColor(0xFFFFFFFF);

        totalAmountTxtview = findViewById(R.id.totalPlayeAmount);

        bestPlayers = findViewById(R.id.bestPlayers);
        first = findViewById(R.id.first);
        second = findViewById(R.id.second);
        third = findViewById(R.id.third);

        firstPlace = findViewById(R.id.firstPlayer);
        secondPlace = findViewById(R.id.secondPlayer);
        thirdPlace = findViewById(R.id.thirdPlayer);

        allPlayersRanking = findViewById(R.id.allPlayersRanking);

        bestPlayers.setVisibility(View.GONE);
        allPlayersRanking.setVisibility(View.GONE);
        first.setVisibility(View.GONE);
        second.setVisibility(View.GONE);
        third.setVisibility(View.GONE);

        scoreList = findViewById(R.id.scoreList);

    }

    private void getTotalAmountFromAllPlayers(){

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("percurso");
        Query query = reference.orderByChild("amount");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                int totalAmount = 0;

                playersId = new LinkedHashSet<String>();

                for (DataSnapshot ds : snapshot.getChildren()){
                    Map< String,  Map< String, Object>> maps = (Map<String,  Map< String, Object>>) ds.getValue();
                    playersId.add(ds.getKey());
                    for ( Map< String, Object> map: maps.values()) {
                        Object total = map.get("amount");
                        int pTotal = Integer.parseInt(String.valueOf(total));
                        totalAmount+=pTotal;
                    }

                }
                    totalAmountTxtview.setText("Total percorrido por todos os jogadores: "+totalAmount);

                    showPlayersScore();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void showPlayersScore() {

        scores = new String[playersId.size()];
        List<Player> players = new ArrayList<Player>();
        index = 0;

        for (String player : playersId){

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("percurso").child(player);
            Query query = reference.orderByChild("amount");
            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    totalScore = 0;
                    String playerName = "";
                    Player player = new Player();

                    for (DataSnapshot ds : snapshot.getChildren()){
                        Map< String, Object> map = (Map<String, Object>) ds.getValue();
                        Object total = map.get("amount");
                        playerName = (String) map.get("username");
                        totalScore += Double.parseDouble(String.valueOf(total));
                    }

                    player.setName(playerName);
                    player.setScore(totalScore);
                    players.add(player);
                    index++;

                    if(index == playersId.size() && playersId.size() > 0)
                        onSuccess(players);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}

            });
        }
    }
    private void showPlayers(List<Player> players) {

        allPlayersRanking.setVisibility(View.VISIBLE);

        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player p1, Player p2) {
                return (int) (p2.getScore() - (int) p1.getScore());
            }
        });

        scores = new String[players.size()];

        int i = 0;
        for (Player player : players) {
            scores[i] = player.getName() + ": " + player.getScore();
            i++;
        }

        adapter = new ArrayAdapter(this, R.layout.activity_listview_item, scores);
        scoreList.setAdapter(adapter);
    }
    private void showBestPlayers(List<Player> players){
        bestPlayers.setVisibility(View.VISIBLE);
            switch (players.size()){

                case 1 :
                    showFirstPlace(players);
                    break;
                case 2:
                    showFirstPlace(players);
                    showSecondPlace(players);
                    break;
                default:
                    showFirstPlace(players);
                    showSecondPlace(players);
                    showThirdPlace(players);
            }
    }


    @Override
    public void onSuccess(List<Player> players) {
        showPlayers(players);
        showBestPlayers(players);
    }

    private void showFirstPlace(List<Player> players){
        firstPlace.setText(players.get(0).getName());
        first.setVisibility(View.VISIBLE);
    }
    private void showSecondPlace(List<Player> players){
        secondPlace.setText(players.get(1).getName());
        second.setVisibility(View.VISIBLE);
    }
    private void showThirdPlace(List<Player> players){
        thirdPlace.setText(players.get(2).getName());
        third.setVisibility(View.VISIBLE);
    }

}