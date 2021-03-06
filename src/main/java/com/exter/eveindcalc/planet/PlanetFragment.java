package com.exter.eveindcalc.planet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.exter.controls.DoubleEditText;
import com.exter.controls.IntegerEditText;
import com.exter.eveindcalc.EICFragmentActivity;
import com.exter.eveindcalc.IEveCalculatorFragment;
import com.exter.eveindcalc.R;
import com.exter.eveindcalc.TaskHelper;
import com.exter.eveindcalc.data.inventory.InventoryDA;
import com.exter.eveindcalc.data.inventory.Item;
import com.exter.eveindcalc.data.planet.Planet;
import com.exter.eveindcalc.data.planet.PlanetDA;
import com.exter.eveindcalc.data.planet.PlanetProduct;
import com.exter.eveindcalc.data.planet.PlanetProductDA;

import java.util.ArrayList;
import java.util.List;

import exter.eveindustry.data.planet.IPlanetBuilding;
import exter.eveindustry.task.PlanetTask;

public class PlanetFragment extends Fragment implements IEveCalculatorFragment
{

  private class PlanetSetClickListener implements OnClickListener
  {
    @Override
    public void onClick(View v)
    {
      if(planet_task == null)
      {
        return;
      }
      Intent in = new Intent(getActivity(), PlanetListActivity.class);
      startActivityForResult(in, REQUEST_PLANET);
    }
  }
  
  private class AddProcessorClickListener implements OnClickListener
  {
    @Override
    public void onClick(View v)
    {
      if(planet_task == null)
      {
        return;
      }
      Intent in = new Intent(getActivity(), PlanetProductListActivity.class);
      in.putExtra("planet", planet_task.getPlanet().getID());
      startActivityForResult(in, REQUEST_PROCESS);
    }
  }

  private class AddExtractorClickListener implements OnClickListener
  {
    @Override
    public void onClick(View v)
    {
      if(planet_task == null)
      {
        return;
      }
      Intent in = new Intent(getActivity(), PlanetResourceListActivity.class);
      in.putExtra("planet", planet_task.getPlanet().getID());
      startActivityForResult(in, REQUEST_PROCESS);
    }
  }

  private class RunTimeChangeWatcher implements IntegerEditText.ValueListener
  {
    @Override
    public void onValueChanged(int new_value)
    {
      if(planet_task == null)
      {
        return;
      }
      planet_task.setRunTime(new_value);
    }
  }

  private class TaxChangeWatcher implements DoubleEditText.ValueListener
  {
    @Override
    public void valueChanged(int tag, double new_value)
    {
      if(planet_task == null)
      {
        return;
      }
      planet_task.setCustomsOfficeTax((float)new_value);
      activity.notiftyExtraExpenseChanged();
    }
  }

  private TextView tx_planet_name;
  private ImageView im_planet_icon;
  private IntegerEditText ed_runtime;
  private DoubleEditText ed_tax;
  PlanetTask planet_task;

  private  EICFragmentActivity activity;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    activity = (EICFragmentActivity)getActivity();
    planet_task = (PlanetTask)activity.getCurrentTask();
    View rootView = inflater.inflate(R.layout.planet, container, false);
    
    ed_runtime = new IntegerEditText((EditText) rootView.findViewById(R.id.ed_planet_runtime), 1, 99999, 0, new RunTimeChangeWatcher());
    ed_tax = new DoubleEditText((EditText) rootView.findViewById(R.id.ed_planet_tax),-1, 0.0, 100.0, 15.0, new TaxChangeWatcher());

    tx_planet_name = (TextView) rootView.findViewById(R.id.tx_planet_type);
    im_planet_icon = (ImageView) rootView.findViewById(R.id.im_planet_icon);
    Button bt_planet_set = (Button) rootView.findViewById(R.id.bt_planet_set);
    Button bt_addprocessor = (Button) rootView.findViewById(R.id.bt_planet_add_processor);
    Button bt_addextractor = (Button) rootView.findViewById(R.id.bt_planet_add_extractor);
    
    bt_planet_set.setOnClickListener(new PlanetSetClickListener());
    bt_addprocessor.setOnClickListener(new AddProcessorClickListener());
    bt_addextractor.setOnClickListener(new AddExtractorClickListener());

    ly_inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    ly_process = (LinearLayout) rootView.findViewById(R.id.ly_planet_process);

    onTaskChanged();
    return rootView;
  }

  
  static private final int REQUEST_PLANET = 0;
  static private final int REQUEST_PROCESS = 1;
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if(resultCode == Activity.RESULT_OK)
    {
      activity.getCurrentTask().registerListener(activity.GetListener());
      switch(requestCode)
      {
        case REQUEST_PLANET:
          planet_task.setPlanet(PlanetDA.getPlanet(data.getIntExtra("planet", -1)));
          break;
        case REQUEST_PROCESS:
          planet_task.addBuilding(PlanetProductDA.getProduct(data.getIntExtra("planetproduct", -1)));
          break;
      }
      onTaskChanged();
    }
  }

  @Override
  public void onTaskChanged()
  {
    if(activity == null || planet_task == null)
    {
      return;
    }
    Planet p = (Planet)planet_task.getPlanet();
    tx_planet_name.setText(p.TypeName);
    TaskHelper.setImageViewItemIcon(im_planet_icon, InventoryDA.getItem(p.ID));
    ed_runtime.setValue(planet_task.getRunTime());
    ed_tax.setValue(planet_task.getCustomsOfficeTax());

    holders = new ArrayList<>();

    ly_process.removeAllViews();
        
    for(IPlanetBuilding pr:planet_task.getBuildings())
    {
      View v = ly_inflater.inflate(R.layout.process, ly_process, false);
      ViewHolderPlanetProcess proc_holder = new ViewHolderPlanetProcess(v,(PlanetProduct)pr);
      ly_process.addView(v);
      holders.add(proc_holder);
    }
  }

  @Override
  public void onPriceValueChanged()
  {

  }

  @Override
  public void onMaterialSetChanged()
  {

  }

  @Override
  public void onMaterialChanged(int item)
  {
    
  }


  private class ViewHolderPlanetProcess
  {
    private class RemoveListener implements OnClickListener
    {
      @Override
      public void onClick(View v)
      {
        planet_task.removeBuilding(building);
        onTaskChanged();
      }
    }

    //private ImageView im_icon;
    private TextView tx_name;
    private ImageButton bt_remove;
    private ImageView im_icon;
    

    private PlanetProduct building;

    public ViewHolderPlanetProcess(View view, PlanetProduct proc)
    {
      building = proc;
      tx_name = (TextView) view.findViewById(R.id.tx_process_name);
      bt_remove = (ImageButton) view.findViewById(R.id.bt_process_remove);
      im_icon = (ImageView) view.findViewById(R.id.im_process_icon);

      if(proc.Materials.size() > 0)
      {
        im_icon.setImageResource(R.drawable.planet_process);
      } else
      {
        im_icon.setImageResource(R.drawable.planet_extractor);
      }
      tx_name.setText(((Item)proc.ProductItem.item).Name);
      bt_remove.setOnClickListener(new RemoveListener());
    }
  }

  private LinearLayout ly_process;

  public List<ViewHolderPlanetProcess> holders;

  private LayoutInflater ly_inflater;

  @Override
  public void onExtraExpenseChanged()
  {

  }

  @Override
  public void onTaskParameterChanged(int param)
  {

  }

}
