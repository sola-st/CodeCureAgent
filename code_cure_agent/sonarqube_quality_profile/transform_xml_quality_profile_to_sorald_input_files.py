import click
import xml.etree.ElementTree as ET
import json

@click.command()
@click.argument(
    "quality-profile-xml-file",
    type=click.File()
)
def transform_xml_quality_profile_to_sorald_input_files(quality_profile_xml_file):
    """Parses the provided SonarQube Java quality profile into a text file that is a list of rules, and a json file with parameter settings for the rules.\n
    These two files are used to run the Sorald SonarQube mining tool using the specific quality profile.\n 
    Specify the exported SonarQube Java quality profile via QUALITY_PROFILE_XML_FILE. The profile must be in the correct xml format, as exported via the SonarQube UI."""
    tree = ET.parse(quality_profile_xml_file)
    root = tree.getroot()
    rule_elements = root.findall(".//rule")

    rule_keys_list = []

    rule_parameters_dict = {}

    for rule in rule_elements:
        rule_key = rule.find(".//key").text
        rule_keys_list.append(rule_key)

        parameters_element = rule.find(".//parameters")
        
        first_item = True
        
        for parameter in parameters_element:
            # Only create the dict item for the rule, if it has parameters specified
            if first_item == True:
                rule_parameters_dict[rule_key] = {}
                first_item = False

            key = parameter.find(".//key").text
            value = parameter.find(".//value").text
            rule_parameters_dict[rule_key][key] = value

        

    with open("quality_profile_rule_keys.txt", "w") as rule_keys_file:
        rule_keys_file.write(",".join(rule_keys_list))

    with open("quality_profile_rule_parameters.json", "w") as rule_parameters_json:
        json.dump(rule_parameters_dict, rule_parameters_json)

if __name__ == "__main__":
    transform_xml_quality_profile_to_sorald_input_files()