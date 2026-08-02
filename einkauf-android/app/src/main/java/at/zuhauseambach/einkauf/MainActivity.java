package at.zuhauseambach.einkauf;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  private final String[] shops={"HOFER","PENNY","BILLA","SPAR"};
  private final String[] items={"Semmeln/Brötchen","Brot","Butter","Marmelade","Honig","Schinken","Aufschnitt","Käse","Eier","Joghurt","Milch","Orangensaft","Kaffee","Tee","Obst","Gemüse","Mineralwasser","Weizenbier"};
  private final String[] brands={"Backbox","Zurück zum Ursprung","Milsani","Darbo","Natur aktiv","Gourmet","Gourmet","Milfina","Zurück zum Ursprung","Milsani","Milsani","Happy Day","Amaroy","Westminster","Obstangebot","Gemüseangebot","Römerquelle","Aktionsmarke"};
  private final String[] defaultShop={"HOFER","HOFER","HOFER","SPAR","HOFER","PENNY","PENNY","HOFER","HOFER","HOFER","HOFER","PENNY","HOFER","HOFER","HOFER","HOFER","BILLA","SPAR"};
  private final int[] qty={20,1,2,1,1,2,2,2,12,8,2,2,1,1,2,2,8,10};
  private LinearLayout list;
  private EditText guests,hosts,days,price;
  private SharedPreferences prefs;

  public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("einkauf",0);build();}
  private TextView tv(String s,int sp,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setPadding(20,12,20,12);if(bold)v.setTypeface(Typeface.DEFAULT_BOLD);return v;}
  private EditText number(String key,String def){EditText e=new EditText(this);e.setInputType(2|8192);e.setText(prefs.getString(key,def));return e;}
  private void build(){ScrollView sc=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,18,18,30);sc.addView(root);
    root.addView(tv("Einkauf Zuhause am Bach",26,true));root.addView(tv("Zwei-Tage-Planung · HOFER → PENNY → BILLA → SPAR",15,false));
    LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);guests=number("guests","2");hosts=number("hosts","2");days=number("days","2");price=number("price","10");
    for(EditText e:new EditText[]{guests,hosts,days,price}){e.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));row.addView(e);}root.addView(row);root.addView(tv("Gäste | Gastgeber | Tage | € je Gast",12,false));
    Button calc=new Button(this);calc.setText("Einkaufsliste berechnen");root.addView(calc);TextView sum=tv("",16,true);root.addView(sum);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);
    calc.setOnClickListener(v->{save();render(sum);});render(sum);setContentView(sc);
  }
  private int val(EditText e,int d){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return d;}}
  private void save(){prefs.edit().putString("guests",guests.getText().toString()).putString("hosts",hosts.getText().toString()).putString("days",days.getText().toString()).putString("price",price.getText().toString()).apply();}
  private void render(TextView sum){list.removeAllViews();int persons=val(guests,2)+val(hosts,2), d=val(days,2);double factor=(persons*d)/8.0;double total=0;
    for(String shop:shops){TextView h=tv(shop,20,true);h.setBackgroundColor(0xffe8efe9);list.addView(h);for(int i=0;i<items.length;i++)if(defaultShop[i].equals(shop)){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);CheckBox c=new CheckBox(this);c.setText(items[i]+" – "+brands[i]);r.addView(c);LinearLayout ed=new LinearLayout(this);ed.setOrientation(LinearLayout.HORIZONTAL);EditText q=number("q"+i,String.valueOf(Math.max(1,(int)Math.ceil(qty[i]*factor))));EditText br=new EditText(this);br.setText(prefs.getString("b"+i,brands[i]));EditText pr=number("p"+i,"0");for(EditText e:new EditText[]{q,br,pr}){e.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));ed.addView(e);}r.addView(ed);r.addView(tv("Menge | Marke/Hersteller | Preis €",11,false));list.addView(r);try{total+=Double.parseDouble(pr.getText().toString().replace(',','.'));}catch(Exception ignored){}
      int idx=i;q.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("q"+idx,q.getText().toString()).apply();});br.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("b"+idx,br.getText().toString()).apply();});pr.setOnFocusChangeListener((v,f)->{if(!f)prefs.edit().putString("p"+idx,pr.getText().toString()).apply();});}}
    double revenue=val(guests,2)*val(days,2)*val(price,10);sum.setText(String.format(Locale.GERMANY,"Frühstücksumsatz: %.2f € · Eingetragene Kosten: %.2f € · Differenz: %.2f €",revenue,total,revenue-total));
  }
}
