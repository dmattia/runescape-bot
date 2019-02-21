package util.common;

import misc.Kitten;
import org.rspeer.runetek.api.component.tab.Spell;
import skills.*;
import util.Globals;
import util.common.Activity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ActivitySelectorView extends JFrame {
    ActivitySelectorModel model;
    JPanel activityConfigView;

    public ActivitySelectorView(ActivitySelectorModel model) {
        super("Activity Selector");

        this.model = model;
        this.activityConfigView = new JPanel();

        setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));

        add(createActivitySelector());
        add(activityConfigView);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                Globals.script.setStopping(true);
            }
        });
        pack();
    }

    private JComboBox createActivitySelector() {
        JComboBox comboBox = new JComboBox(prependDefaultOption(model.getActivityKeys()));
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                model.setActivityByName((String) comboBox.getSelectedItem());
                updateViewForSelectedActivity();
                revalidate();
                pack();
            }
        });
        return comboBox;
    }

    // TODO(dmattia): Finish adding "None Selected" option to dropdown
    private static String[] prependDefaultOption(String[] options) {
        ArrayList<String> list = new ArrayList<>(Arrays.asList(options));
        list.add(0, "None Selected");
        return list.stream().toArray(String[]::new);
    }

    private JComponent getComponentForConfigOptions(ActivityConfigModel configModel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        for (ActivityConfigModel.TextOption textOption : configModel.textOptions) {
            JTextField field = new JTextField(textOption.value);

            field.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    model.getActivity().flatMap(Activity::getConfigModel).ifPresent(config -> {
                        config.keyValStore.put(textOption.key, field.getText());
                    });
                }
            });
            panel.add(field);
        }

        return panel;
    }

    private void updateViewForSelectedActivity() {
        activityConfigView.removeAll();

        model.getActivity()
                .flatMap(Activity::getConfigModel)
                .map(this::getComponentForConfigOptions)
                .ifPresent(activityConfigView::add);
    }
}
