class Entity{
    constructor(entityName,entityPluralName, apiBaseName, json){
        this.entityName = entityName;
        this.entityPluralName = entityPluralName;
        this.apiBaseName = apiBaseName;
        this.json = json;
    }
    show(){
        return api().url("/"+this.apiBaseName+"/show/" + this.json.Id).get();
    }

    destroy(){
        return api().url("/"+this.apiBaseName+"/destroy/" + this.json.Id).get();
    }

    save(){
        return api().url("/"+this.apiBaseName+"/save").post(this.json);
    }

}
class Autocomplete {
    constructor(apiBaseName,target){
        this.apiBaseName = apiBaseName;
        this.jqElement = jQuery(target);
    }

    search(descriptionColumn){
        let field = descriptionColumn;
        let self = this;
        let element = this.jqElement;
        return new Promise(function(resolve,reject){
            let element = self.jqElement;
            let ajaxurl = "/"+self.apiBaseName+"/search";
            if (element.val().length > 3 ) {
                let url = ajaxurl ;
                let val = element.val().replace(/[^a-zA-Z0-9 ]/g, " ")
                if (field){
                    url = url + "/" + field +":" + val + "*";
                }else {
                    url = url + "/" + val;
                }
                api().url(url).get().then(function(response){
                    if (resolve){
                        resolve(response);
                    }
                }).catch(function(err){
                    if (reject){
                        reject(err);
                    }
                });
            }else {
                resolve(undefined);
            }
        });
    }
}