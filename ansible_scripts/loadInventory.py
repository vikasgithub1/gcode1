#!/usr/bin/env python
#  Ansible: initialize needed objects

import os
import sys
import ast
import json
from collections import namedtuple

from ansible.parsing.dataloader import DataLoader
from ansible.vars import VariableManager
from ansible.inventory import Inventory
from ansible.executor.playbook_executor import PlaybookExecutor

variable_manager = VariableManager()
loader = DataLoader()

#  Ansible: Load inventory
inventory = Inventory(
  loader = loader,
  variable_manager = variable_manager,
  host_list = './ansible_scripts/inventory', # Substitute your filename here
)

def serialize(inventory, groupname):
    groupnames = groupname.split(",")
    if not isinstance(inventory, Inventory):
        return dict()

    group_data = dict()
    for group in inventory.get_groups():
        if group in groupnames:
            host_data = list()
            #  Seed host data for group
            for host in inventory.get_group(group).hosts:
                hostObj = host.serialize()
                host_data.append(dict(name=hostObj['name'], ip=host.vars['ansible_ssh_host']))
            groupObj = inventory.get_group(group).serialize()
            group_data[groupObj['name']] = host_data

    return json.dumps(group_data)

#  Calling serialize function with list of environment groups

serialized_inventory = serialize(inventory, sys.argv[1])
print serialized_inventory
