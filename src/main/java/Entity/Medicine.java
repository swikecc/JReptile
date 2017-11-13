package Entity;

import java.util.List;

/**
 * @author Created by swike <swikecc@gmail.com> on 2017/11/9.
 */
public class Medicine {
    List<String> names;
    List<String> values;

    public Medicine(List<String> names, List<String> values) {
        this.names = names;
        this.values = values;
    }

    public List<String> getNames() {
        return names;
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        String result = "";
        for (int i = 0; i < names.size(); i++) {
            result += names.get(i) + " : " + values.get(i) + "\n";
        }
        return result;
    }
}
