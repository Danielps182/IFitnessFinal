package org.ifsp.ifitness;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView amountTxtview;
    private RecyclerView recyclerView;
    private FloatingActionButton fab;

    private FirebaseAuth mAuth;
    private  FirebaseUser user;
    private DatabaseReference ref;
    private String onlineUserId = "";
    private ProgressDialog loader;

    private TodayItemsAdapter todayItemsAdapter;
    private List<Data> myDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (user != null) {
            Toast.makeText(getApplicationContext(), "Bem vindo de volta " + user.getEmail() + "!", Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_main);

        toolbar  = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Distância Tracker");
        getSupportActionBar().getThemedContext();
        toolbar.setTitleTextColor(0xFFFFFFFF);

        amountTxtview = findViewById(R.id.totalAmount);

        fab = findViewById(R.id.fab);

        mAuth = FirebaseAuth.getInstance();
        onlineUserId = mAuth.getCurrentUser().getUid();
        ref = FirebaseDatabase.getInstance().getReference().child("percurso").child(onlineUserId);
        loader = new ProgressDialog(this);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addItemSpentOn();
            }
        });

        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        myDataList = new ArrayList<>();
        todayItemsAdapter = new TodayItemsAdapter(MainActivity.this, myDataList);
        recyclerView.setAdapter(todayItemsAdapter);

       readItems();


    }

    private void readItems(){
        
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("percurso").child(onlineUserId);
        Query query = reference.orderByChild("date");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myDataList.clear();
                for (DataSnapshot dataSnapshot: snapshot.getChildren()){
                    Data data = dataSnapshot.getValue(Data.class);
                    myDataList.add(data);
                }

                todayItemsAdapter.notifyDataSetChanged();

                int totalAmount = 0;
                for (DataSnapshot ds : snapshot.getChildren()){
                    Map< String, Object> map = (Map<String, Object>) ds.getValue();
                    Object total = map.get("amount");
                    int pTotal = Integer.parseInt(String.valueOf(total));
                    totalAmount+=pTotal;

                    amountTxtview.setText("Total Percorrido: "+totalAmount);

                }

                if(myDataList.size() == 0)
                    amountTxtview.setText("Total Percorrido: "+0);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void addItemSpentOn() {

        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);

        View myView = inflater.inflate(R.layout.input_layout, null);

        myDialog.setView(myView);

        final AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);

        final Spinner itemSpinner = myView.findViewById(R.id.spinner);
        ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.items));
        itemsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        itemSpinner.setAdapter(itemsAdapter);

        final EditText amount = myView.findViewById(R.id.amount);
        final EditText notes = myView.findViewById(R.id.note);
        Button cancelBtn = myView.findViewById(R.id.cancel);
        Button saveBtn = myView.findViewById(R.id.save);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String mAmount = amount.getText().toString().trim();
                String note = notes.getText().toString();
                String mItem = itemSpinner.getSelectedItem().toString();

                if (TextUtils.isEmpty(mAmount)){
                    amount.setError("Quantidade percorrida obrigatoria!");
                    return;
                }
                if (TextUtils.isEmpty(note)){
                    notes.setError("Descricao obrigatoria!");
                    return;
                }
                if (mItem.equalsIgnoreCase("Escolha um item")){
                    Toast.makeText(MainActivity.this, "Please select a valid item", Toast.LENGTH_SHORT).show();
                }
                else {
                    loader.setTitle("Adicionar Item de Percurso");
                    loader.setMessage("Aguarde, o item esta sendo adicionado...");
                    loader.setCanceledOnTouchOutside(false);
                    loader.show();

                    String id = ref.push().getKey();
                    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                    Calendar cal = Calendar.getInstance();
                    String date = dateFormat.format(cal.getTime());

                    Data data = new Data(mItem, date, id,note, Integer.parseInt(mAmount), user.getEmail().split("@")[0]);
                    ref.child(id).setValue(data).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(MainActivity.this, "Item adicionado com sucesso", Toast.LENGTH_SHORT).show();
                            }else {
                                Toast.makeText(MainActivity.this, "Falha ao adicionar o item", Toast.LENGTH_SHORT).show();
                            }
                            loader.dismiss();
                        }
                    });

                }

                dialog.dismiss();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case  R.id.account:
                intent = new Intent(MainActivity.this, AccountActivity.class);
                startActivity(intent);
                break;
            case  R.id.rank:
                intent = new Intent(MainActivity.this, RankActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}