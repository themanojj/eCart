package BitsDukan.ekart.shop.model;


import BitsDukan.ekart.shop.helper.Constant;

public class Slider {
    String image;
    String type;
    String type_id;
    String name;

    public Slider(String type, String type_id, String name, String image) {
        this.type = type;
        this.type_id = type_id;
        this.name = name;
        this.image = image;
    }

    public Slider(String image) {
        this.image = image;
    }

    public String getType_id() {
        return type_id;
    }

    public String getName() {
        return name;
    }

    public String getImage()
    {
        return image.replace("http://bitsgalaxy.co/", Constant.MAINBASEUrl);
    }

    public String getType() {
        return type;
    }
}
