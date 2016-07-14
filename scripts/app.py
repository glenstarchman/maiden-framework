from helpers import *
from misc_builders import *

class App:

    def __init__(self, info, model_info):

        self.info = info
        self.app_info = self.info["app"]
        self.db_info = self.info["db"]
        self.social_info = self.info["social"]

        self.model_info = model_info
        #build out our models
        self.models = [Model(model) for model in model_info]

        #build out meta info
        self.name = camelize(self.app_info["name"])
        self.name_lower = underscore(self.name).lower()

        self.namespace = self.app_info["namespace"].lower()
        self.package = "%s.%s" % (self.namespace, self.name_lower)
        self.payload_key = self.app_info.get("payload_key", "payload")
        self.port = self.app_info["port"]
        self.http_port = self.app_info.get("https_port", "")
        self.certificate_path = self.app_info.get("certificate_path", "")
        self.key_path = self.app_info.get("key_path", "")

        self.source_directory = self.app_info["source_directory"]
        self.base_path = os.path.join(self.source_directory, "src/main/scala/%s" % (self.name_lower))
        self.config_path = os.path.join(self.source_directory, "config")
        self.environment = self.app_info.get("envirornment", "development")

        #make sure our paths exist

        print("Creating directories under %s" % (self.source_directory))
        make_dir(self.config_path)
        make_dir(os.path.join(self.source_directory, "project"))
        make_dir(self.base_path)
        for d in ('api', 'encoders', "migrations", "models"):
            make_dir(os.path.join(self.base_path, d))


        #build out our data structures
        self.security = security_info(self.app_info)
        #self.database  = build_database(self.app_info)
        self.social = self.build_social()

    def build_security(self):
        pass

    def build_database(self):
        pass

    def build_social(self):
        pass

class Model:
    def __init__(self, model_info):
        self.info = model_info

        self.name = camelize(self.info["name"])
        self.name_lower = camelize(self.info["name"], False)
        self.db_name = self.info.get("db_name", underscore(self.name).lower())
        self.build_columns()

    def build_columns(self):
        self.columns = [Column(self, column) for column in self.info["columns"]]
        print self.columns

class ForeignKey:

    def __init__(self, col):
        self.ref_table = col.info["references"]["table"]
        self.ref_column = col.info["references"]["column"]
        self.column = col.db_name
        self.table = col.table
        self.field_name = col.name

        self.on_delete = camelize(col.info["references"].get("on_delete", "SetDefault"))
        self.name = "fk_%s_%s" % (self.ref_table, self.ref_column)

class Column:
    def __init__(self, model, col_info):

        self.info = col_info
        self.table = model.db_name
        self.model = model.name

        self.name = camelize(self.info["name"], False)
        self.named_type = self.info["type"]
        db_map = DB_MAP[self.info["type"]]
        self.db_type = DB_MAP[self.named_type]["db"]
        self.index = self.info.get("index", False)
        self.limit = self.info.get("limit", None)
        self.db_name = self.info.get("db_name", col_info["name"])
        self.scala_type = db_map["scala"]
        self.validations = col_info.get("validations", [])
        if col_info.get("formatters"): print col_info["formatters"]
        self.formatters = col_info.get("formatters", [])

        self.index = col_info.get("index", False)
        self.unique_index = col_info.get("unique_index", False)

        if 'validations' in db_map: self.validations.extend(db_map["validations"])
        #if 'formatters' in db_map: self.formatters.extend(db_map["formatters"])

        self.build_references()
        self.build_migration_modifiers()


    def build_references(self):
        self.references = None
        if 'references' in self.info:
            self.references = ForeignKey(self)

    def build_migration_modifiers(self):
        modifiers = ["NotNull"]
        if exists_and_true(self.info, 'auto_increment'): modifiers.append("AutoIncrement")
        if exists_and_true(self.info, 'primary_key'): modifiers.append("PrimaryKey")
        if exists_and_true(self.info, 'nullable'): modifiers.remove("NotNull")
        if 'default' in self.info: modifiers.append("""Default("%s")""" % (self.info['default']))
        if 'limit' in self.info: modifiers.append("""Limit(%s)""" % (self.info['limit']))

        self.migration_modifiers = ', '.join(modifiers)