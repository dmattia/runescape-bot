package util.common;

import misc.Kitten;
import org.rspeer.runetek.api.component.tab.Spell;
import skills.*;
import util.Globals;
import util.common.Activity;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ActivitySelectorView extends JFrame {
    ActivitySelectorModel model;

    public ActivitySelectorView(ActivitySelectorModel model) {
        super("Activity Selector");

        this.model = model;
        add(createActivitySelector());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                Globals.script.setStopping(true);
            }
        });
        pack();
    }

    private JComboBox createActivitySelector() {
        JComboBox comboBox = new JComboBox(model.getActivityKeys());
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                model.setActivityByName((String) comboBox.getSelectedItem());
            }
        });
        return comboBox;
    }
}
